package org.wxd.boot.batis.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.io.TemplatePack;
import org.wxd.boot.batis.DataWrapper;
import org.wxd.boot.batis.EntityField;
import org.wxd.boot.batis.EntityTable;
import org.wxd.boot.batis.code.CodeLan;
import org.wxd.boot.batis.code.CreateJavaCode;
import org.wxd.boot.batis.enums.ColumnType;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.collection.OfSet;
import org.wxd.boot.field.ClassMapping;
import org.wxd.boot.field.ClassWrapper;
import org.wxd.boot.lang.ConvertUtil;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.str.json.FastJsonUtil;
import org.wxd.boot.system.MarkTimer;
import org.wxd.boot.timer.MyClock;

import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.*;

/**
 * 读取excel
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-01-13 15:06
 **/
@Slf4j
public abstract class ExcelRead<DM extends EntityTable, DW extends DataWrapper<DM>> extends Excel implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * excel 文件数据
     */
    protected Map<String, DM> excelDataTableMap;
    /** 数据类型起始行号 */
    protected int dataTypeRowIndex = 1;
    /** 字段名字起始行号 */
    protected int dataNameRowIndex = 2;
    /** 字段描述 */
    protected int dataDescRowIndex = 3;
    /** 扩展内容 通常 server, client, all, no */
    protected int dataExtendRowIndex = 4;
    /** 数据起始行号 */
    protected int dataStartRowIndex = 5;

    protected boolean loopFind = false;
    protected String[] excelPaths = null;

    protected Set<String> haveExtend = new HashSet<>();
    protected String[] splitStrs = {",|，", ":|："};

    /** 读取excel文件读取字段的权限 server, client, all, no */
    public ExcelRead loadExcel(String... haveExtends) {
        MarkTimer timerMark = MarkTimer.build();
        this.haveExtend = OfSet.asSet(haveExtends);
        this.excelDataTableMap = new LinkedHashMap<>();
        List<File> excelFiles = new LinkedList<>();
        for (String excelPath : excelPaths) {
            FileUtil.walkFiles(excelPath, ".xls", ".xlsx").forEach(excelFiles::add);
        }

        if (excelFiles.isEmpty()) {
            throw new RuntimeException("目录下面并未找到 .xls .xlsx");
        }
        log.info("特别提示：Excel sheetName - 需要是 q_ 开头 才能解析");
        LinkedHashMap<String, Workbook> workbooks = new LinkedHashMap<>();
        try {
            for (File excelFile : excelFiles) {
                Workbook workbook = builderWorkbook(excelFile);
                if (workbook != null) {
                    /*刷新公式*/
                    workbook.setForceFormulaRecalculation(true);
                    /*强制计算*/
                    workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
                    workbooks.put(excelFile.getName(), workbook);
                }
            }
            for (Map.Entry<String, Workbook> entry : workbooks.entrySet()) {
                readExcelHead(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, Workbook> entry : workbooks.entrySet()) {
                readExcelRow(entry.getKey(), entry.getValue());
            }
            log.info("表结构数量：" + excelDataTableMap.size() + ", " + timerMark.execTime2String());
        } finally {
            for (Workbook workbook : workbooks.values()) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    log.error("关闭资源", e);
                }
            }
        }
        return this;
    }

    /**
     * 读取表头
     */
    protected ExcelRead readExcelHead(String excelFile, Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            /*只读取第一个*/
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName().trim().toLowerCase();

            if (StringUtil.emptyOrNull(sheetName)
                    || sheetName.startsWith("sheet")
                    || sheetName.contains("@")
                    || sheetName.contains("$")
                    || !sheetName.startsWith("q_")) {
                log.debug("Excel文件不能解析：" + excelFile + ", sheetName=" + sheetName + " - 需要是 q_ 开头 sheet name 才能解析");
                continue;
            }

            Cell tableCommentCall = sheet.getRow(0).getCell(0);

            String tableComment = getCellString(tableCommentCall, true);

            Row rowDataType = sheet.getRow(dataTypeRowIndex);
            Row rowDataName = sheet.getRow(dataNameRowIndex);
            Row rowDataDesc = sheet.getRow(dataDescRowIndex);
            Row rowDataExtend = sheet.getRow(dataExtendRowIndex);

            if (rowDataType == null
                    || rowDataName == null
                    || rowDataDesc == null
                    || rowDataExtend == null) {
                log.info(
                        "文件：" + excelFile
                                + " sheetName " + sheetName
                                + " 表头数据不合法无法解析"
                );
                continue;
            }

            short lastCellNum = rowDataType.getLastCellNum();

            DM entityTable = excelDataTableMap.get(sheetName);

            if (entityTable == null) {
                entityTable = createDataStruct();
            } else {
                if (entityTable.getColumns().size() != lastCellNum) {
                    log.info("Excel文件数据列不一致：\n"
                            + "新文件：" + excelFile
                            + ", sheetName=" + sheetName
                            + ", 数据列：" + lastCellNum
                            + "\n原文件：" + entityTable.getTableComment()
                            + ", sheetName=" + entityTable.getTableName()
                            + ", 数据列：" + entityTable.getColumns().size());
                }
            }

            entityTable.setTableName(sheetName);
            entityTable.setTableComment(excelFile + " - " + sheetName + " - " + tableComment);

            HashSet<String> columnSet = new HashSet<>();
            for (int c = 0; c < lastCellNum; c++) {
                Cell cellDataType = rowDataType.getCell(c);
                Cell cellDataName = rowDataName.getCell(c);
                Cell cellDataDesc = rowDataDesc.getCell(c);
                Cell cellDataExtend = rowDataExtend.getCell(c);
                if (cellDataType != null && cellDataName != null && cellDataDesc != null) {
                    builderTable(entityTable, columnSet, cellDataType, cellDataName, cellDataDesc, cellDataExtend);
                }
            }
            if (!entityTable.getColumns().isEmpty()) {
                excelDataTableMap.put(entityTable.getTableName(), entityTable);
//                log.info("Excel文件解析完成：" + excelFile
//                        + ", sheetName=" + sheetName
//                        + ", 数据列：" + entityTable.getColumns().size());
            } else {
                log.info(
                        "可能 是不需要处理的 Excel 文件：" + excelFile
                                + ", sheetName=" + sheetName
                );
            }
        }
        return this;
    }

    /**
     * @param entityTable
     * @param cellDataType   第一行的列，是数据类型，
     * @param cellDataName   第二行的列，是数据的模型列名
     * @param cellDataDesc   第三行的列，描述，备注
     * @param cellDataExtend 标注是客户端还是服务器
     */
    protected void builderTable(DM entityTable,
                                HashSet<String> columnSet,
                                Cell cellDataType,
                                Cell cellDataName,
                                Cell cellDataDesc,
                                Cell cellDataExtend) {
        /*数据类型*/
        String cellType = getCellString(cellDataType, false);
        /*数据映射名字*/
        String cellName = getCellString(cellDataName, true);
        String cellExtend = getCellString(cellDataExtend, false);

        if (StringUtil.emptyOrNull(cellName)
                || StringUtil.emptyOrNull(cellType)
                || "no".equalsIgnoreCase(cellExtend)) {
            return;
        }

        if (StringUtil.notEmptyOrNull(cellExtend) && !"all".equalsIgnoreCase(cellExtend)) {
            if (!this.haveExtend.isEmpty() && !this.haveExtend.contains(cellExtend)) {
                return;
            }
        }

        if (!columnSet.add(cellName)) {
            throw new RuntimeException("文件：" + entityTable.getLogTableName() + ", 存在相同的字段名：" + cellName);
        }

        EntityField entityField = new EntityField();

        entityField.setColumnName(cellName);
        if ("id".equalsIgnoreCase(cellName) || entityTable.getDataColumnKey() == null) {/*解析excel表默认第一个字段是主键*/
            entityField.setColumnKey(true);
            entityTable.setDataColumnKey(entityField);
        }

        entityField.setColumnLength(5000);

        getDataWrapper().buildColumnType(entityField, cellType);

        if (entityField.getColumnType() == null
                || entityField.getColumnType() == ColumnType.None) {
            entityField.setColumnType(ColumnType.Text);
            entityField.setFieldType(String.class);
        }

        /*描述*/
        entityField.setColumnComment(getCellString(cellDataDesc, false));
        /*扩展*/
        entityField.setColumnExtend(cellExtend);

        EntityField oldEntityField = entityTable.getColumnMap().get(cellName);
        if (oldEntityField == null) {
            entityTable.getColumnMap().put(entityField.getColumnName(), entityField);
        } else {
            if (!oldEntityField.getFieldTypeString().equalsIgnoreCase(entityField.getFieldTypeString())) {
                throw new RuntimeException("两张表字段类型不一致：" + oldEntityField.getFieldTypeString() + ", " + entityField.getFieldTypeString());
            }
        }
    }

    /**
     * 读取文件内容
     */
    protected void readExcelRow(String excelFile, Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName().trim().toLowerCase();

            if (StringUtil.emptyOrNull(sheetName)) {
                continue;
            }

            DM entityTable = excelDataTableMap.get(sheetName);
            if (entityTable == null) {
                continue;
            }

            Collection<EntityField> columns = entityTable.getColumns();

            Row rowDataType = sheet.getRow(dataTypeRowIndex);
            Row rowDataName = sheet.getRow(dataNameRowIndex);
            Row rowDataExtend = sheet.getRow(dataExtendRowIndex);

            short lastCellNum = rowDataName.getLastCellNum();
            int lastRowNum = sheet.getLastRowNum();
            for (int r = dataStartRowIndex; r <= lastRowNum; r++) {

                Map<EntityField, Object> readRowMap = new LinkedHashMap<>();
                Row row = sheet.getRow(r);
                if (row == null) {
                    break;
                }

                boolean nullRow = true;
                for (int c = 0; c < lastCellNum; c++) {
                    Cell rowCellData = row.getCell(c);
                    if (StringUtil.notEmptyOrNull(getCellString(rowCellData, false))) {
                        nullRow = false;
                    }
                }

                if (nullRow) {
                    continue;
                }

                for (int c = 0; c < lastCellNum; c++) {
                    /*数据类型*/
                    String cellType = getCellString(rowDataType.getCell(c), false);
                    /*数据映射名字*/
                    String cellName = getCellString(rowDataName.getCell(c), true);

                    if (StringUtil.emptyOrNull(cellType)
                            || StringUtil.emptyOrNull(cellName)) {
                        /*忽略字段*/
                        continue;
                    }

                    EntityField entityField = entityTable.getColumnMap().get(cellName);
                    if (entityField == null) {
                        /*忽略字段*/
                        continue;
                    }
                    Cell rowCellData = row.getCell(c);
                    readRowColumn(entityTable, r + 1, readRowMap, entityField, rowCellData);
                }

                boolean checkAllCellNull = true;
                LinkedHashMap<EntityField, Object> rowMap = new LinkedHashMap<>();
                for (EntityField column : columns) {
                    final Object value = readRowMap.get(column);
                    rowMap.put(column, value);
                    if (value != null) {
                        final String valueOf = String.valueOf(value);
                        if (!"0".equalsIgnoreCase(valueOf) && StringUtil.notEmptyOrNull(valueOf))
                            checkAllCellNull = false;
                    }
                }
                if (checkAllCellNull) {
                    /*没有数据了*/
                    break;
                }
                entityTable.getRows().add(rowMap);
            }
            log.info("Excel文件解析完成：" + excelFile + ", sheetName=" + sheetName +
                    ", 数据列：" + entityTable.getColumns().size() +
                    ", 数量行：" + entityTable.getRows().size());
        }
    }

    /**
     * @param entityTable
     * @param rowMap
     * @param entityField 第二行的列，是数据的模型列名
     * @param rowCellData 每一行的数据
     */
    protected void readRowColumn(DM entityTable, int rowNumber, Map<EntityField, Object> rowMap, EntityField entityField, Cell rowCellData) {
        Object cellValue = getCellValue(entityTable, rowNumber, entityField, rowCellData);
        if (cellValue instanceof String) {
            final String toString = cellValue.toString();
            if (toString.endsWith(".0")) {
                cellValue = toString.substring(0, toString.length() - 2);
            }
        }
        rowMap.put(entityField, cellValue);
    }

    protected Object getCellValue(DM entityTable, int rowNumber, EntityField entityField, Cell cellData) {
        /*空白的话，根据传入的类型返回默认值*/
        String trim = "";
        try {
            if (cellData != null) {

                /*以下是判断数据的类型*/
                switch (cellData.getCellType()) {
                    case NUMERIC: /*数字*/
                        DecimalFormat df = new DecimalFormat("0");
                        trim = df.format(cellData.getNumericCellValue());
                        break;
                    case STRING: /*字符串*/
                        trim = cellData.getStringCellValue();
                        break;
                    case BOOLEAN: /*Boolean*/
                        trim = String.valueOf(cellData.getBooleanCellValue());
                        break;
                    case FORMULA: /*公式*/ {
                        /*得到对应单元格的字符串*/
                        try {
                            trim = String.valueOf(cellData.getNumericCellValue());
                        } catch (IllegalStateException e) {
                            trim = String.valueOf(cellData.getRichStringCellValue());
                        }
                    }
                    break;
                    case BLANK: /*空值*/
                        trim = "";
                        break;
                    case ERROR: /*故障*/
                        trim = "非法字符";
                        break;
                    default:
                        trim = "未知类型";
                        break;
                }

                switch (entityField.getFieldTypeString().toLowerCase()) {
                    case "byte[]": {
                        byte[] arrays;
                        if (notNullOrEmpty(trim)) {
                            if (trim.startsWith("[") && trim.endsWith("]")) {
                                arrays = FastJsonUtil.parse(trim.replace('|', ','), byte[].class);
                            } else {
                                String[] split = trim.split(splitStrs[1]);
                                arrays = new byte[split.length];
                                for (int i = 0; i < split.length; i++) {
                                    arrays[i] = Double.valueOf(split[i]).byteValue();
                                }
                            }
                        } else {
                            arrays = new byte[0];
                        }
                        return arrays;
                    }
                    case "byte[][]": {
                        byte[][] arrays;
                        if (notNullOrEmpty(trim)) {
                            if (trim.startsWith("[") && trim.endsWith("]")) {
                                arrays = FastJsonUtil.parse(trim.replace('|', ','), byte[][].class);
                            } else {
                                String[] split0 = trim.split(splitStrs[0]);
                                arrays = new byte[split0.length][];
                                for (int i0 = 0; i0 < split0.length; i0++) {
                                    String[] split1 = split0[i0].split(splitStrs[1]);
                                    byte[] integers = new byte[split1.length];
                                    for (int i1 = 0; i1 < split1.length; i1++) {
                                        integers[i1] = Double.valueOf(split1[i1]).byteValue();
                                    }
                                    arrays[i0] = integers;
                                }
                            }
                        } else {
                            arrays = new byte[0][];
                        }
                        return arrays;
                    }
                    case "int[]": {
                        int[] arrays;
                        if (notNullOrEmpty(trim)) {
                            if (trim.startsWith("[") && trim.endsWith("]")) {
                                arrays = FastJsonUtil.parse(trim.replace('|', ','), int[].class);
                            } else {
                                String[] split = trim.split(splitStrs[1]);
                                arrays = new int[split.length];
                                for (int i = 0; i < split.length; i++) {
                                    arrays[i] = Double.valueOf(split[i]).intValue();
                                }
                            }
                        } else {
                            arrays = new int[0];
                        }
                        return arrays;
                    }
                    case "int[][]": {
                        int[][] arrays;
                        if (notNullOrEmpty(trim)) {
                            if (trim.startsWith("[") && trim.endsWith("]")) {
                                arrays = FastJsonUtil.parse(trim.replace('|', ','), int[][].class);
                            } else {
                                String[] split0 = trim.split(splitStrs[0]);
                                arrays = new int[split0.length][];
                                for (int i0 = 0; i0 < split0.length; i0++) {
                                    String[] split1 = split0[i0].split(splitStrs[1]);
                                    int[] integers = new int[split1.length];
                                    for (int i1 = 0; i1 < split1.length; i1++) {
                                        integers[i1] = Double.valueOf(split1[i1]).intValue();
                                    }
                                    arrays[i0] = integers;
                                }
                            }
                        } else {
                            arrays = new int[0][];
                        }
                        return arrays;
                    }
                    case "long[]": {
                        long[] arrays;
                        if (notNullOrEmpty(trim)) {
                            if (trim.startsWith("[") && trim.endsWith("]")) {
                                arrays = FastJsonUtil.parse(trim.replace('|', ','), long[].class);
                            } else {
                                String[] split = trim.split(splitStrs[1]);
                                arrays = new long[split.length];
                                for (int i = 0; i < split.length; i++) {
                                    arrays[i] = Double.valueOf(split[i]).longValue();
                                }
                            }
                        } else {
                            arrays = new long[0];
                        }
                        return arrays;
                    }
                    case "long[][]": {
                        long[][] arrays;
                        if (notNullOrEmpty(trim)) {
                            if (trim.startsWith("[") && trim.endsWith("]")) {
                                arrays = FastJsonUtil.parse(trim.replace('|', ','), long[][].class);
                            } else {
                                String[] split0 = trim.split(splitStrs[0]);
                                arrays = new long[split0.length][];
                                for (int i0 = 0; i0 < split0.length; i0++) {
                                    String[] split1 = split0[i0].split(splitStrs[1]);
                                    long[] vs1 = new long[split1.length];
                                    for (int i1 = 0; i1 < split1.length; i1++) {
                                        vs1[i1] = Double.valueOf(split1[i1]).longValue();
                                    }
                                    arrays[i0] = vs1;
                                }
                            }
                        } else {
                            arrays = new long[0][];
                        }
                        return arrays;
                    }
                    case "float[]": {
                        float[] arrays;
                        if (notNullOrEmpty(trim)) {
                            if (trim.startsWith("[") && trim.endsWith("]")) {
                                arrays = FastJsonUtil.parse(trim.replace('|', ','), float[].class);
                            } else {
                                String[] split = trim.split(splitStrs[1]);
                                arrays = new float[split.length];
                                for (int i = 0; i < split.length; i++) {
                                    arrays[i] = Double.valueOf(split[i]).floatValue();
                                }
                            }
                        } else {
                            arrays = new float[0];
                        }
                        return arrays;
                    }
                    case "float[][]": {
                        float[][] arrays;
                        if (notNullOrEmpty(trim)) {
                            if (trim.startsWith("[") && trim.endsWith("]")) {
                                arrays = FastJsonUtil.parse(trim.replace('|', ','), float[][].class);
                            } else {
                                String[] split0 = trim.split(splitStrs[0]);
                                arrays = new float[split0.length][];
                                for (int i0 = 0; i0 < split0.length; i0++) {
                                    String[] split1 = split0[i0].split(splitStrs[1]);
                                    float[] vs1 = new float[split1.length];
                                    for (int i = 0; i < split1.length; i++) {
                                        vs1[i] = Double.valueOf(split1[i]).floatValue();
                                    }
                                    arrays[i0] = vs1;
                                }
                            }
                        } else {
                            arrays = new float[0][];
                        }
                        return arrays;
                    }
                    case "string[]": {
                        String[] arrays;
                        if (notNullOrEmpty(trim)) {
                            if (trim.startsWith("[") && trim.endsWith("]")) {
                                arrays = FastJsonUtil.parse(trim.replace('|', ','), String[].class);
                            } else {
                                arrays = trim.split(splitStrs[1]);
                            }
                        } else {
                            arrays = new String[0];
                        }
                        return arrays;
                    }
                    case "string[][]": {
                        String[][] arrays;
                        if (notNullOrEmpty(trim)) {
                            if (trim.startsWith("[") && trim.endsWith("]")) {
                                arrays = FastJsonUtil.parse(trim.replace('|', ','), String[][].class);
                            } else {
                                String[] split0 = trim.split(splitStrs[0]);
                                arrays = new String[split0.length][];
                                for (int i = 0; i < split0.length; i++) {
                                    arrays[i] = split0[i].split(splitStrs[1]);
                                }
                            }
                        } else {
                            arrays = new String[0][];
                        }
                        return arrays;
                    }
                    case "list<bool>":
                    case "list<boolean>":
                    case "arraylist<boolean>": {
                        List<Boolean> list;
                        if (notNullOrEmpty(trim)) {
                            list = FastJsonUtil.parseArray(trim.replace('|', ','), Boolean.class);
                        } else {
                            list = new ArrayList<>();
                        }
                        return list;
                    }
                    case "list<byte>":
                    case "arraylist<byte>": {
                        List<Byte> list;
                        if (notNullOrEmpty(trim)) {
                            list = FastJsonUtil.parseArray(trim.replace('|', ','), Byte.class);
                        } else {
                            list = new ArrayList<>();
                        }
                        return list;
                    }
                    case "list<int>":
                    case "list<integer>":
                    case "arraylist<int>":
                    case "arraylist<integer>": {
                        List<Integer> list;
                        if (notNullOrEmpty(trim)) {
                            list = FastJsonUtil.parseArray(trim.replace('|', ','), Integer.class);
                        } else {
                            list = new ArrayList<>();
                        }
                        return list;
                    }
                    case "list<long>":
                    case "arraylist<long>": {
                        List<Long> list;
                        if (notNullOrEmpty(trim)) {
                            list = FastJsonUtil.parseArray(trim.replace('|', ','), Long.class);
                        } else {
                            list = new ArrayList<>();
                        }
                        return list;
                    }
                    case "list<string>":
                    case "arraylist<string>": {
                        List<String> list;
                        if (notNullOrEmpty(trim)) {
                            list = FastJsonUtil.parseArray(trim.replace('|', ','), String.class);
                        } else {
                            list = new ArrayList<>();
                        }
                        return list;
                    }
                    case "set<byte>": {
                        Set<Byte> list;
                        if (notNullOrEmpty(trim)) {
                            list = new LinkedHashSet<>(FastJsonUtil.parseArray(trim.replace('|', ','), Byte.class));
                        } else {
                            list = new LinkedHashSet<>();
                        }
                        return list;
                    }
                    case "set<int>": {
                        Set<Integer> list;
                        if (notNullOrEmpty(trim)) {
                            list = new LinkedHashSet<>(FastJsonUtil.parseArray(trim.replace('|', ','), Integer.class));
                        } else {
                            list = new LinkedHashSet<>();
                        }
                        return list;
                    }
                    case "set<long>": {
                        Set<Long> list;
                        if (notNullOrEmpty(trim)) {
                            list = new LinkedHashSet<>(FastJsonUtil.parseArray(trim.replace('|', ','), Long.class));
                        } else {
                            list = new LinkedHashSet<>();
                        }
                        return list;
                    }
                    case "set<string>": {
                        Set<String> list;
                        if (notNullOrEmpty(trim)) {
                            list = new LinkedHashSet<>(FastJsonUtil.parseArray(trim.replace('|', ','), String.class));
                        } else {
                            list = new LinkedHashSet<>();
                        }
                        return list;
                    }
                    default:
                        return ConvertUtil.changeType(trim, entityField.getFieldType());
                }
            }
            return ConvertUtil.defaultValue(entityField.getFieldType());
        } catch (Exception ex) {
            final RuntimeException runtimeException = new RuntimeException(
                    ex.getMessage()
                            + "\n文件：" + entityTable.getTableComment()
                            + ";\nsheet：" + entityTable.getTableName()
                            + ";\n列：" + entityField.getColumnName()
                            + ";\n行：" + rowNumber
                            + ";\n数据类型：" + entityField.getFieldTypeString().toLowerCase()
                            + ";\n数据：" + trim + "----无法转换：" + entityField.getFieldType());
            runtimeException.setStackTrace(ex.getStackTrace());
            throw runtimeException;
        }
    }

    boolean notNullOrEmpty(String source) {
        if (StringUtil.notEmptyOrNull(source)) {
            if (!"#null".equalsIgnoreCase(source)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取一列的字符
     *
     * @param data
     * @param isColumnName 如果是列名，需要转化首字母小写
     * @return
     */
    protected String getCellString(Cell data, boolean isColumnName) {
        String trim = "";
        if (data != null) {
            /*空白的话，根据传入的类型返回默认值*/
            /*默认类型*/
            if (data.getCellType() == CellType.STRING
                    || (data.getCellType() == CellType.FORMULA && data.getCachedFormulaResultType() == CellType.STRING)) {
                /*字符类型*/
                trim = data.getStringCellValue().trim();
            }
            if (StringUtil.emptyOrNull(trim)) {
                trim = data.toString().trim();
            }
        }
        if (StringUtil.notEmptyOrNull(trim)) {
            trim = trim.replace("class", "clazz")
                    .replace("-", "_");
            if (isColumnName) {
                trim = StringUtil.lowerFirst(trim);
            }
        }
        return trim.trim();
    }

    public abstract DW getDataWrapper();

    protected abstract DM createDataStruct();

    /**
     * 保存数据
     *
     * @return
     */
    protected ExcelRead saveData() {
        final Collection<DM> values = excelDataTableMap.values();
        for (DM entityTable : values) {
            saveData(entityTable);
            log.warn("导出 文件 " + entityTable.getTableComment()
                    + ", 表名 " + entityTable.getTableName()
                    + ", 数据 " + entityTable.getRows().size() + " 条");
        }
        return this;
    }

    protected abstract ExcelRead saveData(DM entityTable);

    public ExcelRead createCode(CodeLan codeLan, String saveCodePath, String savePackageName) {
        TemplatePack templatePack = TemplatePack.build(CreateJavaCode.class.getClassLoader(), "template/orm/code/" + codeLan.name().toLowerCase());
        createCode(templatePack, codeLan.getHouZhui(), saveCodePath, savePackageName);
        return this;
    }

    public ExcelRead createCode(CodeLan codeLan, String saveCodePath, String savePackageName, DM entityTable) {
        TemplatePack templatePack = TemplatePack.build(CreateJavaCode.class.getClassLoader(), "template/orm/code/" + codeLan.name().toLowerCase());
        createCode(templatePack, codeLan.getHouZhui(), saveCodePath, savePackageName, entityTable);
        return this;
    }

    /**
     * 生成实体模型
     *
     * @param saveCodePath    保存代码文件路径
     * @param savePackageName 保存代码文件的时候 名字空间
     * @return
     */
    public ExcelRead createCode(TemplatePack templatePack, String houZhui, String saveCodePath, String savePackageName) {

        if (saveCodePath != null && getDataWrapper() != null) {
            final Collection<DM> values = excelDataTableMap.values();
            for (DM entityTable : values) {
                createCode(templatePack, houZhui, saveCodePath, savePackageName, entityTable);
            }
        }

        return this;
    }

    /**
     * jpa
     *
     * @param entityTable 模型映射关系
     * @param savePath    保存路径，前缀
     * @param packageName 包名 会自动转化成路径
     * @return
     * @throws Exception
     */
    public ExcelRead createCode(TemplatePack templatePack, String houZhui, String savePath, String packageName, DM entityTable) {
        if (!savePath.endsWith("/")) {
            savePath += "/";
        }
        String tmpPath = savePath + packageName.replace(".", "/") + "/";

        ObjMap parse = new ObjMap();
        parse.put("packageName", packageName);
        parse.put("date", MyClock.nowString()/*当前时间*/);
        parse.put("tableName", entityTable.getTableName());
        parse.put("tableComment", entityTable.getTableComment());
        parse.put("codeClassName", entityTable.getCodeClassName());
        ArrayList<Map<String, Object>> columns = new ArrayList<>();
        for (EntityField field : entityTable.getColumnMap().values()) {
            ClassMapping wrapper = ClassWrapper.wrapper(field.getClass());
            Map<String, Object> column = wrapper.toMap(field);
            column.put("fieldNameLower", field.getFieldName());
            column.put("fieldNameUpper", StringUtil.upperFirst(field.getFieldName()));
            columns.add(column);
        }
        parse.put("columns", columns);

        File file;
        {
            file = new File(tmpPath + "bean/mapping/" + entityTable.getCodeClassName() + "Mapping." + houZhui);
            templatePack.ftl2File("bean-mapping.ftl", parse, file.getPath());
            log.info("生成 映射 文件：" + entityTable.getTableComment() + ", " + entityTable.getTableName() + ", " + FileUtil.getCanonicalPath(file));
        }
        file = new File(tmpPath + "bean/" + entityTable.getCodeClassName() + "Bean." + houZhui);
        if (!file.exists()) {
            templatePack.ftl2File("bean.ftl", parse, file.getPath());
            log.info("生成 扩展 文件：" + entityTable.getTableComment() + ", " + entityTable.getTableName() + ", " + FileUtil.getCanonicalPath(file));
        }

        file = new File(tmpPath + "factory/" + entityTable.getCodeClassName() + "Factory." + houZhui);
        if (!file.exists()) {
            templatePack.ftl2File("factory.ftl", parse, file.getPath());
            log.info("生成 工厂 文件：" + entityTable.getTableComment() + ", " + entityTable.getLogTableName() + "Factory, " + FileUtil.getCanonicalPath(file));
        }
        return this;
    }

    /**
     * 展示数据
     */
    public ExcelRead showData() {
        excelDataTableMap.values().forEach((entityTable) -> log.warn(entityTable.toDataString()));
        return this;
    }

    /**
     * 读取所有的数据行
     */
    public <R> List<R> readList(Class<R> clazz) throws Exception {
        List<R> list = new LinkedList<>();
        final DW dataWrapper = getDataWrapper();
        EntityTable entityTable = dataWrapper.asEntityTable(clazz);
        EntityTable textDataMapping = excelDataTableMap.get(entityTable.getTableName());
        final LinkedList<LinkedHashMap<EntityField, Object>> rows = textDataMapping.getRows();
        Set<String> columnNameSet = new HashSet<>();
        for (LinkedHashMap<EntityField, Object> row : rows) {
            R instance = clazz.getConstructor().newInstance();
            for (Map.Entry<EntityField, Object> dataColumnObjectEntry : row.entrySet()) {
                final String columnName = dataColumnObjectEntry.getKey().getColumnName();
                final EntityField entityField = entityTable.getColumnMap().get(columnName);
                if (entityField == null) {
                    if (columnNameSet.add(columnName)) {
                        log.info("结构：" + clazz.getName() + ", 缺少映射字段：" + columnName);
                    }
                    continue;
                }
                try {
                    Object convertDataValue = dataWrapper.fromDbValue(entityField, dataColumnObjectEntry.getValue());
                    if (entityField.isFinalField()) {
                        if (Map.class.isAssignableFrom(entityField.getFieldType())) {
                            final Map fieldValue = (Map) entityField.getFieldValue(instance);
                            fieldValue.putAll((Map) convertDataValue);
                        } else if (List.class.isAssignableFrom(entityField.getFieldType())) {
                            final List fieldValue = (List) entityField.getFieldValue(instance);
                            fieldValue.addAll((List) convertDataValue);
                        } else if (Set.class.isAssignableFrom(entityField.getFieldType())) {
                            final Set fieldValue = (Set) entityField.getFieldValue(instance);
                            fieldValue.addAll((Set) convertDataValue);
                        } else {
                            throw new RuntimeException(
                                    clazz.getName() + ", "
                                            + entityField.getFieldName()
                                            + ", " + dataColumnObjectEntry.getValue()
                                            + " \n数据库配置值：" + convertDataValue + "; 最终类型异常"
                            );
                        }
                    } else {
                        entityField.setFieldValue(instance, convertDataValue);
                    }
                } catch (Exception e) {
                    throw Throw.as(clazz.getName() + ", " + entityField.getFieldName() + ", " + dataColumnObjectEntry.getValue(), e);
                }
            }
            list.add(instance);
        }
        return list;
    }

    public ExcelRead<DM, DW> excelPaths(String... excelPaths) {
        excelPaths(false, excelPaths);
        return this;
    }

    /** 设置读取目录 */
    public ExcelRead<DM, DW> excelPaths(boolean loop, String... excelPaths) {
        this.loopFind = loop;
        this.excelPaths = excelPaths;
        return this;
    }

    /**
     * 行号从0开始
     *
     * @param dataNameRowIndex   映射字段名起始行号
     * @param dataTypeRowIndex   映射字段数据类型
     * @param dataDescRowIndex   映射字段名起始行号
     * @param dataExtendRowIndex 扩展内容 通常 server, client, all, no
     * @param dataRowIndex       数据起始行号
     * @return
     */
    public ExcelRead<DM, DW> dataIndex(int dataNameRowIndex,
                                       int dataTypeRowIndex,
                                       int dataDescRowIndex,
                                       int dataExtendRowIndex,
                                       int dataRowIndex) {
        if (dataNameRowIndex == 0
                || dataTypeRowIndex == 0
                || dataDescRowIndex == 0
                || dataExtendRowIndex == 0
                || dataRowIndex == 0) {
            throw new RuntimeException("第一行必须是表的注释，起始行应该是1开始也就是第二行");
        }
        this.dataNameRowIndex = dataNameRowIndex;
        this.dataTypeRowIndex = dataTypeRowIndex;
        this.dataDescRowIndex = dataDescRowIndex;
        this.dataExtendRowIndex = dataExtendRowIndex;
        this.dataStartRowIndex = dataRowIndex;
        return this;
    }

    /**
     * 数据起始行号
     * <p>行号从0开始
     */
    public ExcelRead<DM, DW> dataStartRowIndex(int dataStartRowIndex) {
        this.dataStartRowIndex = dataStartRowIndex;
        return this;
    }

    /**
     * 映射字段名
     * <p>行号从0开始
     */
    public ExcelRead<DM, DW> dataTypeRowIndex(int dataTypeRowIndex) {
        this.dataTypeRowIndex = dataTypeRowIndex;
        return this;
    }

    /**
     * 映射字段名
     * <p>行号从0开始
     */
    public ExcelRead<DM, DW> dataNameRowIndex(int dataNameRowIndex) {
        this.dataNameRowIndex = dataNameRowIndex;
        return this;
    }

    /**
     * 描述
     * <p>行号从0开始
     */
    public ExcelRead<DM, DW> dataDescRowIndex(int dataDescRowIndex) {
        this.dataDescRowIndex = dataDescRowIndex;
        return this;
    }

    /**
     * 扩展内容
     * <p>通常 server, client, all, no
     * <p>行号从0开始
     */
    public ExcelRead<DM, DW> dataExtendRowIndex(int dataExtendRowIndex) {
        this.dataExtendRowIndex = dataExtendRowIndex;
        return this;
    }

    public Map<String, DM> getExcelDataTableMap() {
        return excelDataTableMap;
    }

}

