package wxdgaming.boot.batis.sql;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot.agent.exception.Throw;
import wxdgaming.boot.agent.io.FileUtil;
import wxdgaming.boot.agent.zip.OutZipFile;
import wxdgaming.boot.agent.zip.ReadZipFile;
import wxdgaming.boot.batis.DataBuilder;
import wxdgaming.boot.batis.DataHelper;
import wxdgaming.boot.batis.DbConfig;
import wxdgaming.boot.core.append.StreamWriter;
import wxdgaming.boot.core.collection.ObjMap;
import wxdgaming.boot.core.str.json.FastJsonUtil;
import wxdgaming.boot.core.system.MarkTimer;
import wxdgaming.boot.core.timer.MyClock;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
@Slf4j
public abstract class SqlDataHelper<DM extends SqlEntityTable, DW extends SqlDataWrapper<DM>>
        extends DataHelper<DM, DW>
        implements SqlExecute<DM, DW>, SqlTable<DM, DW>, SqlSelect<DM, DW>, SqlDel<DM, DW> {

    protected SqlBatchPool batchPool = null;
    /** 数据库集合 */
    protected List<String> dbBaseList = new ArrayList<>();

    /** 数据库表和表说明备注 */
    protected Map<String, String> dbTableMap = new LinkedHashMap<>();
    /** 数据库，数据结构 */
    protected Map<String, LinkedHashMap<String, ObjMap>> dbTableStructMap = new LinkedHashMap<>();

    protected SqlDataHelper() {
    }

    /**
     * @param dbConfig
     */
    public SqlDataHelper(DW dataWrapper, DbConfig dbConfig) {
        super(dataWrapper, dbConfig);
    }

    public SqlDataHelper initBatchPool(int batchThreadSize) {
        if (batchPool == null) {
            this.batchPool = new SqlBatchPool(this, this.getDbBase() + "-BatchJob", batchThreadSize);
        } else {
            log.error("已经初始化了 db Batch Pool", new RuntimeException());
        }

        return this;
    }

    public SqlBatchPool getBatchPool() {
        return batchPool;
    }

    @Override
    public DW getDataWrapper() {
        return super.getDataWrapper();
    }

    public SqlQueryBuilder queryBuilder() {
        return getDataWrapper().queryBuilder();
    }

    /**
     * 获取数据库的链接
     *
     * @return
     */
    @Override
    public Connection getConnection() {
        return getConnection(this.getDbConfig().getDbHost());
    }

    /**
     * @param dbnameString
     * @return
     */
    @Override
    public Connection getConnection(String dbnameString) {
        try {
            return DriverManager.getConnection(
                    getConnectionString(dbnameString),
                    this.getDbConfig().getDbUser(),
                    this.getDbConfig().getDbPwd()
            );
        } catch (SQLException e) {
            throw Throw.as(e);
        }
    }

    public List<String> getDbBaseList() {
        if (dbBaseList.isEmpty()) {
            String sql = "SELECT `SCHEMA_NAME` FROM information_schema.SCHEMATA;";
            try (Connection connection = getConnection("information_schema");
                 ResultSet resultSet = connection.prepareStatement(sql).executeQuery();) {
                while (resultSet.next()) {
                    String string = resultSet.getString("SCHEMA_NAME");
                    dbBaseList.add(string);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return dbBaseList;
    }

    public Map<String, String> getDbTableMap() {
        if (dbTableMap == null) {
            dbTableMap = new LinkedHashMap<>();
        }
        if (dbTableMap.isEmpty()) {
            String sql = "SELECT TABLE_NAME,TABLE_COMMENT FROM information_schema.`TABLES` WHERE table_schema= ? ORDER BY TABLE_NAME";
            final List<ObjMap> jsonObjects = this.query(sql, this.getDbBase());
            for (ObjMap jsonObject : jsonObjects) {
                final String table_name = jsonObject.getString("TABLE_NAME");
                final String TABLE_COMMENT = jsonObject.getString("TABLE_COMMENT");
                dbTableMap.put(table_name, TABLE_COMMENT);
            }
        }
        return dbTableMap;
    }

    @Override
    public Map<String, LinkedHashMap<String, ObjMap>> getDbTableStructMap() {
        if (dbTableStructMap == null) {
            dbTableStructMap = new LinkedHashMap<>();
        }
        if (dbTableStructMap.isEmpty()) {
            String sql =
                    "SELECT" +
                    "    TABLE_NAME," +
                    "    COLUMN_NAME," +
                    "    ORDINAL_POSITION," +
                    "    COLUMN_DEFAULT," +
                    "    IS_NULLABLE," +
                    "    DATA_TYPE," +
                    "    CHARACTER_MAXIMUM_LENGTH," +
                    "    NUMERIC_PRECISION," +
                    "    NUMERIC_SCALE," +
                    "    COLUMN_TYPE," +
                    "    COLUMN_KEY," +
                    "    EXTRA," +
                    "    COLUMN_COMMENT \n" +
                    "FROM information_schema.`COLUMNS`\n" +
                    "WHERE table_schema= ? \n" +
                    "ORDER BY TABLE_NAME, ORDINAL_POSITION;";

            final List<ObjMap> jsonObjects = this.query(sql, this.getDbBase());
            for (ObjMap jsonObject : jsonObjects) {
                final String table_name = jsonObject.getString("TABLE_NAME");
                final String column_name = jsonObject.getString("COLUMN_NAME");
                dbTableStructMap.computeIfAbsent(table_name, l -> new LinkedHashMap<>())
                        .put(column_name, jsonObject);
            }
        }
        return dbTableStructMap;
    }

    public void setPreparedParams(PreparedStatement statement, DataBuilder dataBuilder) throws Exception {
        dataWrapper.setPreparedParams(statement, dataBuilder);
    }

    public void setPreparedParams(PreparedStatement statement, DM dataMapping, Object obj) throws Exception {
        dataWrapper.setPreparedParams(statement, dataMapping, obj);
    }

    @Override
    public void close() {
        super.close();
        if (this.getBatchPool() != null) {
            try {
                this.getBatchPool().close();
            } catch (Throwable throwable) {
                log.error(this.toString() + " batch pool close", throwable);
            }
        }
    }

    /** 通过sql语句获得当前数据库名字 */
    public String dataBase() {
        return executeScalar("select database();", String.class);
    }

    /** insert */
    public int insert(Object model) {
        DM entityTable = asEntityTable(model);
        String sqlStr = entityTable.getInsertSql(model);
        return executeUpdate(entityTable, model, sqlStr);
    }

    /** insertBatch */
    public int insertBatch(List<?> models) {
        if (models == null || models.isEmpty()) return -1;
        Object model = models.getFirst();
        DM entityTable = asEntityTable(model);
        String insertSql = entityTable.getInsertSql(model);
        MarkTimer markTimer = MarkTimer.build();
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                for (Object o : models) {
                    setPreparedParams(stmt, entityTable, o);
                    stmt.addBatch();
                    stmt.clearParameters();
                }
                int size = Arrays.stream(stmt.executeBatch()).sum();
                connection.commit();
                float execTime = markTimer.execTime();
                if (getSqlDao().getDbConfig().isShow_sql()) {
                    log.info("\n" + insertSql + "\n 结果：" + size + ", 耗时：" + execTime + " ms");
                } else if (execTime > 10000) {
                    log.warn("\n" + insertSql + "\n 结果：" + size + ", 耗时：" + execTime + " ms");
                }
                return size;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw Throw.as(insertSql, e);
        }
    }

    /** update */
    public int update(Object model) {
        DM entityTable = asEntityTable(model);
        String sqlStr = entityTable.getUpdateSql(model);
        return executeUpdate(entityTable, model, sqlStr);
    }

    /** updateBatch */
    public int updateBatch(List<?> models) {
        if (models == null || models.isEmpty()) return -1;
        Object model = models.getFirst();
        DM entityTable = asEntityTable(model);
        String updateSql = entityTable.getUpdateSql(model);
        MarkTimer markTimer = MarkTimer.build();
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                for (Object o : models) {
                    setPreparedParams(stmt, entityTable, o);
                    stmt.addBatch();
                    stmt.clearParameters();
                }
                int size = Arrays.stream(stmt.executeBatch()).sum();
                connection.commit();
                float execTime = markTimer.execTime();
                if (getSqlDao().getDbConfig().isShow_sql()) {
                    log.info("\n" + updateSql + "\n 结果：" + size + ", 耗时：" + execTime + " ms");
                } else if (execTime > 10000) {
                    log.warn("\n" + updateSql + "\n 结果：" + size + ", 耗时：" + execTime + " ms");
                }
                return size;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw Throw.as(updateSql, e);
        }
    }

    /**
     * mysql 特有的方式 replace into 如果之前没有数据就是插入，如果有数据就是更新
     * <p>注意表必须是有主键字段或者唯一字段，否则会出现重复数据
     */
    public int replace(Object model) {
        DM entityTable = asEntityTable(model);
        String sqlStr = entityTable.getReplaceSql(model);
        return executeUpdate(entityTable, model, sqlStr);
    }

    /**
     * mysql 特有的方式 replace into 如果之前没有数据就是插入，如果有数据就是更新
     * <p>注意表必须是有主键字段或者唯一字段，否则会出现重复数据
     */
    public int replaceBatch(List<?> models) {
        if (models == null || models.isEmpty()) return -1;
        Object model = models.getFirst();
        DM entityTable = asEntityTable(model);
        String sqlStr = entityTable.getReplaceSql(model);
        MarkTimer markTimer = MarkTimer.build();
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement stmt = connection.prepareStatement(sqlStr)) {
                for (Object o : models) {
                    setPreparedParams(stmt, entityTable, o);
                    stmt.addBatch();
                    stmt.clearParameters();
                }
                int size = Arrays.stream(stmt.executeBatch()).sum();
                connection.commit();
                float execTime = markTimer.execTime();
                if (getSqlDao().getDbConfig().isShow_sql()) {
                    log.info("\n" + sqlStr + "\n 结果：" + size + ", 耗时：" + execTime + " ms");
                } else if (execTime > 10000) {
                    log.warn("\n" + sqlStr + "\n 结果：" + size + ", 耗时：" + execTime + " ms");
                }
                return size;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw Throw.as(sqlStr, e);
        }
    }

    protected int executeUpdate(DM entityTable, Object model, String sqlStr) {
        return stmtFun(
                stmt -> {
                    setPreparedParams(stmt, entityTable, model);
                    return stmt.executeUpdate();
                },
                sqlStr
        );
    }

    /**
     * 把数据库所有表内容，导出到文件
     *
     * @param fileDir 目录
     */
    public String outDb2File(String fileDir) throws Exception {
        try (StreamWriter streamWriter = new StreamWriter()) {
            streamWriter.write("备份数据库：").write(getDbBase()).writeLn();
            String zipFile = MyClock.formatDate(MyClock.SDF_YYYYMMDDHHMMSS_3) + ".zip";
            streamWriter.write("备份文件：").write(zipFile).writeLn();
            fileDir += "/" + getDbBase() + "/" + getDbBase() + "-" + zipFile;
            AtomicLong atomicLong = new AtomicLong();
            try (OutZipFile outZipFile = new OutZipFile(fileDir)) {
                Collection<String> tableNames = this.getDbTableStructMap().keySet();
                for (String tableName : tableNames) {
                    outZipFile.newZipEntry(tableName);
                    atomicLong.set(0);
                    this.query("SELECT * FROM `" + tableName + "`", new String[0],
                            (row) -> {
                                String toString = FastJsonUtil.toJsonWriteType(row);
                                toString += "\n";
                                outZipFile.write(toString);
                                atomicLong.incrementAndGet();
                                return true;
                            }
                    );
                    streamWriter.write("数据表：").write(tableName).write(" - 数据行：").write(atomicLong.get()).writeLn();
                }
            }
            String toString = streamWriter.toString();
            log.info(toString);
            return toString;
        }
    }

    /**
     * 从文件加载到数据库
     *
     * @param zipFile
     */
    public String inDb4File(String zipFile, int batchSize) {
        StringBuilder stringBuilder = new StringBuilder();
        MarkTimer markTimer = MarkTimer.build();
        try (ReadZipFile readZipFile = new ReadZipFile(zipFile)) {
            readZipFile.forEach((tableName, bytes) -> {
                        String s = new String(bytes, StandardCharsets.UTF_8);
                        LinkedList<String> fileLines = new LinkedList<>();
                        AtomicLong size = new AtomicLong();
                        s.lines().forEach(line -> {
                            fileLines.add(line);
                            if (fileLines.size() >= batchSize) {
                                inDb4File(tableName, fileLines);
                                size.addAndGet(fileLines.size());
                                fileLines.clear();
                            }
                        });
                        if (!fileLines.isEmpty()) {
                            inDb4File(tableName, fileLines);
                            size.addAndGet(fileLines.size());
                        }
                        String msg = "从文件：" + zipFile + ", 还原表：" + tableName + ", 数据：" + size.get() + " 完成";
                        stringBuilder.append(msg).append("\n");
                        log.warn(msg);
                    }
            );
        }
        String msg = "所有数据 导入 完成：" + FileUtil.getCanonicalPath(zipFile) + ", 耗时：" + markTimer.execTime() + " ms";
        log.info(msg);
        stringBuilder.append(msg);
        return stringBuilder.toString();
    }

    protected void inDb4File(String tableName, List<String> fileLines) {
        Object[] keys = null;
        LinkedList<Object[]> paramList = new LinkedList<>();
        for (String line : fileLines) {
            ObjMap jsonObject = ObjMap.parse(line);
            TreeMap<Object, Object> stringObjectTreeMap = new TreeMap<>(jsonObject);
            if (keys == null || keys.length < stringObjectTreeMap.size()) {
                keys = stringObjectTreeMap.keySet().toArray(new Object[0]);
            }
            Object[] params = stringObjectTreeMap.values().toArray(new Object[0]);
            paramList.add(params);
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("REPLACE INTO `" + tableName + "` (");
        int i = 0;
        for (Object key : keys) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append("`").append(key).append("`");
            i++;
        }
        stringBuilder.append(") values (");
        i = 0;
        for (Object key : keys) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append("?");
            i++;
        }
        stringBuilder.append(")");
        this.executeBatch(stringBuilder.toString(), paramList);
    }


}
