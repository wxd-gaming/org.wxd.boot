package org.wxd.boot.batis.sql;

import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.function.PredicateE;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.lang.ConvertUtil;
import org.wxd.boot.lang.Tuple2;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-05-06 16:40
 **/
interface SqlExecute<DM extends SqlEntityTable, DW extends SqlDataWrapper<DM>> extends ISql<DM, DW> {

    /**
     * 存储过程
     *
     * @param call
     * @param params
     * @return
     */
    default int prepareCall(String call, Object... params) {
        try (Connection connection = getConnection()) {
            try (CallableStatement prepareCall = connection.prepareCall("{call " + call + "}")) {
                for (int i = 0; i < params.length; i++) {
                    getDataWrapper().setStmtParams(prepareCall, i + 1, params[i]);
                }
                return prepareCall.executeUpdate();
            }
        } catch (Exception e) {
            throw Throw.as("存储过程：" + call, e);
        }
    }

    /**
     * 调用存储过程
     *
     * @param call   function_xxx(?,?,?,?)
     * @param params
     */
    default List<ObjMap> prepareCallQuery(String call, Object... params) {
        List<ObjMap> rows = new LinkedList<>();
        try (Connection connection = getConnection()) {
            try (CallableStatement prepareCall = connection.prepareCall("{call " + call + "}")) {
                for (int i = 0; i < params.length; i++) {
                    getDataWrapper().setStmtParams(prepareCall, i + 1, params[i]);
                }
                try (ResultSet resultSet = prepareCall.executeQuery()) {
                    if (resultSet != null) {
                        while (resultSet.next()) {
                            ObjMap rowMap = new ObjMap();
                            int columnCount = resultSet.getMetaData().getColumnCount();
                            for (int j = 1; j < columnCount + 1; j++) {
                                Object object = resultSet.getObject(j);
                                String columnName = resultSet.getMetaData().getColumnLabel(j);
                                rowMap.put(columnName, object);
                            }
                            rows.add(rowMap);
                        }
                    }
                }
            }
            return rows;
        } catch (Exception e) {
            throw Throw.as("存储过程：" + call, e);
        }
    }

    default long executeBatch(String sqlString, Collection<Object[]> paramList) {
        return stmtFun(stmt -> {
                    for (Object[] params : paramList) {
                        for (int i = 0; i < params.length; i++) {
                            getDataWrapper().setStmtParams(stmt, i + 1, params[i]);
                        }
                        stmt.addBatch();
                        stmt.clearParameters();
                    }
                    final int[] ints = stmt.executeBatch();
                    return Arrays.stream(ints).mapToLong(v -> v).sum();
                },
                sqlString
        );
    }

    default List<ObjMap> query(SqlQueryBuilder sqlQueryBuilder) {
        List<ObjMap> rows = new LinkedList<>();
        Tuple2<String, Object[]> build = sqlQueryBuilder.buildSelect();
        query(build.getLeft(), build.getRight(),
                (row) -> {
                    rows.add(row);
                    return true;
                }
        );
        return rows;
    }

    default void query(SqlQueryBuilder sqlQueryBuilder, PredicateE<ObjMap> call) {
        Tuple2<String, Object[]> build = sqlQueryBuilder.buildSelect();
        query(build.getLeft(), build.getRight(), call);
    }

    /**
     * 返回查询结果集
     *
     * @param sqlString
     * @param params
     * @return
     */
    default List<ObjMap> query(String sqlString, Object... params) {
        List<ObjMap> rows = new LinkedList<>();
        query(sqlString, params,
                (row) -> {
                    rows.add(row);
                    return true;
                }
        );
        return rows;
    }

    /**
     * @param sqlString
     * @param params
     * @param rowCall   返回 true 表现继续查询
     */
    default void query(String sqlString, Object[] params, PredicateE<ObjMap> rowCall) {
        queryResultSet(
                sqlString,
                params,
                (resultSet) -> {
                    ObjMap rowMap = new ObjMap();
                    int columnCount = resultSet.getMetaData().getColumnCount();
                    for (int j = 1; j < columnCount + 1; j++) {
                        Object object = resultSet.getObject(j);
                        String columnName = resultSet.getMetaData().getColumnLabel(j);
                        rowMap.put(columnName, object);
                    }
                    return rowCall.test(rowMap);
                }
        );
    }

    /**
     * @param sqlString
     * @param params
     * @param rowCall   返回 true 表现继续查询
     */
    default void queryResultSet(String sqlString, Object[] params, PredicateE<ResultSet> rowCall) {
        stmtFun((stmt) -> {
                    int size = 0;
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        while (resultSet.next()) {
                            size++;
                            if (!rowCall.test(resultSet)) {
                                break;
                            }
                        }
                    }
                    return size;
                },
                sqlString,
                params
        );
    }

    /**
     * 更新数据
     *
     * @param sql
     * @param args
     * @return
     */
    default int executeUpdate(String sql, Object... args) {
        return stmtFun(PreparedStatement::executeUpdate, sql, args);
    }

    /**
     * 返回第一行，和第一列
     *
     * @param <R>
     * @param sqlString 完整的sql语句
     * @param outClazz  获取后的类型
     * @param args
     * @return
     */
    default <R> R executeScalar(String sqlString, Class<R> outClazz, Object... args) {
        return stmtFun(
                stmt -> {
                    try (ResultSet executeQuery = stmt.executeQuery()) {
                        if (executeQuery.next()) {
                            return (R) ConvertUtil.changeType(executeQuery.getObject(1), outClazz);
                        }
                    }
                    return null;
                },
                sqlString,
                args
        );
    }

    /**
     * 返回所有行的第一列
     *
     * @param <R>
     * @param sqlString 范例 a=? and b=? 或者 a=? or a=?
     * @param outClazz  获取后的类型
     * @param params
     * @return
     */
    default <R> List<R> executeScalarList(String sqlString, Class<R> outClazz, Object... params) {
        List<R> objects = new LinkedList<>();
        queryResultSet(
                sqlString,
                params,
                row -> objects.add((R) ConvertUtil.changeType(row.getObject(1), outClazz))
        );
        return objects;
    }

}
