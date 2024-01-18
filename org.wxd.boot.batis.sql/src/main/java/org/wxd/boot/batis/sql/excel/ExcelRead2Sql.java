package org.wxd.boot.batis.sql.excel;

import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.io.TemplatePack;
import org.wxd.boot.append.StreamWriter;
import org.wxd.boot.batis.EntityField;
import org.wxd.boot.batis.code.CodeLan;
import org.wxd.boot.batis.excel.ExcelRead;
import org.wxd.boot.batis.sql.SqlDataHelper;
import org.wxd.boot.batis.sql.SqlDataWrapper;
import org.wxd.boot.batis.sql.SqlEntityTable;
import org.wxd.boot.str.json.FastJsonUtil;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 读取excel
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-01-13 15:06
 **/
@Slf4j
public class ExcelRead2Sql extends ExcelRead<SqlEntityTable, SqlDataWrapper<SqlEntityTable>> implements Serializable {


    // public static void main(String[] args) throws Exception {
    //     DbConfig dbConfig = new DbConfig()
    //             .setDbHost("192.168.30.254").setDbPort(3306).setDbBase("test")
    //             .setDbUser("root").setDbPwd("ty$2020@mysql")
    //             .setConnectionPool(true);
    //
    //     MysqlDataHelper mysqlDao = new MysqlDataHelper(dbConfig);
    //     mysqlDao.createDatabase();
    //     mysqlDao.initBatchPool(1);
    //     ExcelRead2Sql.builder()
    //             .excelPaths(false, "D:\\2021-03-25\\94联盟航海.xlsx")
    //             .loadExcel()
    //             .showData()
    //             .createCode(CodeLan.Java, "src\\com\\qy\\mg\\dbmodel\\po\\data", "com.qy.mg.dbmodel.po.data")
    //             .saveData("d:/out/sql", mysqlDao);
    // }

    public static ExcelRead2Sql builder() throws Exception {
        return new ExcelRead2Sql();
    }

    /*保存sql 文件*/
    private String saveSqlPath;
    /*如果要导入数据库*/
    private SqlDataHelper<SqlEntityTable, SqlDataWrapper<SqlEntityTable>> sqlDao;

    @Override
    public SqlDataWrapper<SqlEntityTable> getDataWrapper() {
        return this.sqlDao.getDataWrapper();
    }

    @Override
    protected SqlEntityTable createDataStruct() {
        return new SqlEntityTable(this.sqlDao.getDataWrapper());
    }

    @Override
    public ExcelRead2Sql loadExcel(String... haveExtends) {
        super.loadExcel(haveExtends);
        return this;
    }

    @Override
    public ExcelRead2Sql showData() {
        super.showData();
        return this;
    }

    @Override public ExcelRead2Sql createCode(CodeLan codeLan, String saveCodePath, String savePackageName) {
        super.createCode(codeLan, saveCodePath, savePackageName);
        return this;
    }

    @Override
    public ExcelRead2Sql createCode(TemplatePack templatePack, String houZhui, String saveCodePath, String savePackageName) {
        super.createCode(templatePack, houZhui, saveCodePath, savePackageName);
        return this;
    }

    public ExcelRead2Sql saveData(SqlDataHelper sqlDao) throws Exception {
        saveData(null, sqlDao);
        return this;
    }

    public ExcelRead2Sql saveData(String saveSqlPath, SqlDataHelper sqlDao) throws Exception {
        this.saveSqlPath = saveSqlPath;
        this.sqlDao = sqlDao;
        super.saveData();
        return this;
    }

    @Override
    protected ExcelRead2Sql saveData(SqlEntityTable entityTable) {
        if (saveSqlPath != null) {
            getDataWrapper().saveSqlFile(entityTable, saveSqlPath);
        }

        if (sqlDao != null) {
            StreamWriter streamWriter = new StreamWriter();
            try {
                streamWriter.clear();
                getDataWrapper().buildSqlDropTable(streamWriter, entityTable.getTableName());
                sqlDao.executeUpdate(streamWriter.toString());
                streamWriter.clear();
                getDataWrapper().buildSqlCreateTable(streamWriter, entityTable, entityTable.getTableName(), entityTable.getTableComment());
                sqlDao.executeUpdate(streamWriter.toString());
                if (!entityTable.getRows().isEmpty()) {
                    String replaceSql = entityTable.getReplaceSql(null);
                    try (Connection connection = sqlDao.getConnection();
                         PreparedStatement statement = connection.prepareStatement(replaceSql)) {
                        LinkedList<LinkedHashMap<EntityField, Object>> rows = entityTable.getRows();
                        for (Map<EntityField, Object> row : rows) {
                            try {
                                statement.clearParameters();
                                int j = 1;
                                for (Map.Entry<EntityField, Object> dataColumnObjectEntry : row.entrySet()) {
                                    EntityField entityField = dataColumnObjectEntry.getKey();
                                    Object value = dataColumnObjectEntry.getValue();
                                    final Object sqlValue = getDataWrapper().toDbValue(entityField, value);
                                    getDataWrapper().setStmtParams(statement, j, sqlValue);
                                    j++;
                                }
                                statement.addBatch();
                            } catch (Exception e) {
                                log.warn("数据库：" + sqlDao.getDbBase() + ", "
                                        + entityTable.getLogTableName() + "\n"
                                        + replaceSql + "\n" + FastJsonUtil.toJsonFmt(row.values()), e);
                            }
                        }
                        statement.executeBatch();
                        if (sqlDao.getDbConfig().isShow_sql()) {
                            log.warn("\n数据库：" + sqlDao.getDbBase() + ", " + entityTable.getLogTableName() + "\n" + replaceSql + "\n影响行数：" + rows.size());
                        } else {
                            log.warn("数据库：" + sqlDao.getDbBase() + ", " + entityTable.getLogTableName() + ", 影响行数：" + rows.size());
                        }
                    }
                }
            } catch (Throwable throwable) {
                throw Throw.as("数据结构：" + entityTable.getLogTableName(), throwable);
            }
        }
        return this;
    }

    @Override
    public ExcelRead2Sql excelPaths(String... excelPaths) {
        super.excelPaths(excelPaths);
        return this;
    }

    /**
     * 设置读取目录
     *
     * @param loop
     * @param excelPaths
     */
    @Override
    public ExcelRead2Sql excelPaths(boolean loop, String... excelPaths) {
        super.excelPaths(loop, excelPaths);
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
    @Override
    public ExcelRead2Sql dataIndex(int dataNameRowIndex,
                                   int dataTypeRowIndex,
                                   int dataDescRowIndex,
                                   int dataExtendRowIndex,
                                   int dataRowIndex) {
        super.dataIndex(
                dataNameRowIndex,
                dataTypeRowIndex,
                dataDescRowIndex,
                dataExtendRowIndex,
                dataRowIndex
        );
        return this;
    }

    /**
     * 数据起始行号
     * <p>行号从0开始
     *
     * @param dataStartRowIndex
     */
    @Override
    public ExcelRead2Sql dataStartRowIndex(int dataStartRowIndex) {
        super.dataStartRowIndex(dataStartRowIndex);
        return this;
    }

    /**
     * 映射字段名
     * <p>行号从0开始
     *
     * @param dataTypeRowIndex
     */
    @Override
    public ExcelRead2Sql dataTypeRowIndex(int dataTypeRowIndex) {
        super.dataTypeRowIndex(dataTypeRowIndex);
        return this;
    }

    /**
     * 映射字段名
     * <p>行号从0开始
     *
     * @param dataNameRowIndex
     */
    @Override
    public ExcelRead2Sql dataNameRowIndex(int dataNameRowIndex) {
        super.dataNameRowIndex(dataNameRowIndex);
        return this;
    }

    /**
     * 描述
     * <p>行号从0开始
     *
     * @param dataDescRowIndex
     */
    @Override
    public ExcelRead2Sql dataDescRowIndex(int dataDescRowIndex) {
        super.dataDescRowIndex(dataDescRowIndex);
        return this;
    }

    /**
     * 扩展内容
     * <p>通常 server，client， all
     * <p>行号从0开始
     *
     * @param dataExtendRowIndex
     */
    @Override
    public ExcelRead2Sql dataExtendRowIndex(int dataExtendRowIndex) {
        super.dataExtendRowIndex(dataExtendRowIndex);
        return this;
    }

}

