package org.wxd.boot.batis.sql;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.batis.BatchPool;
import org.wxd.boot.batis.DataBuilder;
import org.wxd.boot.batis.DataWrapper;
import org.wxd.boot.batis.DbConfig;
import org.wxd.boot.str.json.FastJsonUtil;
import org.wxd.boot.system.GlobalUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * 异步队列处理插入更新
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-07-29 10:33
 **/
public class SqlBatchPool extends BatchPool {

    private static final Logger log = LoggerFactory.getLogger(SqlBatchPool.class);
    protected SqlDataHelper<SqlEntityTable, SqlDataWrapper<SqlEntityTable>> dataHelper;

    public SqlBatchPool(SqlDataHelper dataHelper, String threadName, int batchThreadSize) {
        super(dataHelper.getDbConfig().getName() + "-" + threadName, batchThreadSize);
        this.dataHelper = dataHelper;
    }

    @Override
    protected DataWrapper dataBuilder() {
        return dataHelper.getDataWrapper();
    }

    @Override
    protected DbConfig dbConfig() {
        return dataHelper.getDbConfig();
    }

    @Override
    public int exec(String tableName, List<DataBuilder> values) throws Exception {
        int i = 0;
        if (values != null && !values.isEmpty()) {
            final DataBuilder dbBase = values.get(0);
            final SqlEntityTable entityTable = (SqlEntityTable) dbBase.getEntityTable();
            String sqlStr = entityTable.getReplaceSql(dbBase.getData());
            try (Connection connection = dataHelper.getConnection()) {
                connection.setAutoCommit(false);
                try (PreparedStatement stm = connection.prepareStatement(sqlStr)) {
                    i += replace(stm, values, sqlStr);
                    stm.clearBatch();
                }
                connection.commit();
            } catch (Exception e) {
                throw Throw.as(sqlStr, e);
            }
        }
        return i;
    }

    private int replace(PreparedStatement stm,
                        List<DataBuilder> values,
                        String sqlStr) {
        int j = 0;
        try {
            for (DataBuilder value : values) {
                stm.clearParameters();
                dataHelper.setPreparedParams(stm, value);
                stm.addBatch();
                j++;
            }
            stm.executeBatch();
        } catch (Exception tex) {
            log.error("数据库操作 replace 对象异常：\n" + sqlStr + ", \n", tex);
            GlobalUtil.exception(sqlStr, tex);
            j = 0;
            try {
                    /*
                    批量提交失败，为了保证数据，然后单独提交再次执行
                     但是必须的清理掉原来的批处理
                     */
                stm.clearBatch();
            } catch (SQLException ex) {
                log.error("数据库操作 replace 对象 clearBatch 异常：" + sqlStr, ex);
            }
            for (DataBuilder value : values) {
                try {
                    stm.clearParameters();
                    dataHelper.setPreparedParams(stm, value);
                    stm.executeBatch();
                    stm.clearBatch();
                } catch (Exception ex) {
                    String valueJsonString = FastJsonUtil.toJsonWriteType(value);
                    String msg = "数据库操作 replace 对象异常\nSql=" + sqlStr + ", \n数据=" + valueJsonString + "\n";
                    log.error(msg, ex);
                    GlobalUtil.exception(msg, tex);
                }
                j++;
            }
        }
        return j;
    }


}
