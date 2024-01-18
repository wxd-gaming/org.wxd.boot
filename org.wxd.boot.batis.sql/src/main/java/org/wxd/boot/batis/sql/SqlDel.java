package org.wxd.boot.batis.sql;


import java.util.Collection;
import java.util.Set;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-06-01 16:39
 **/
interface SqlDel<DM extends SqlEntityTable, DW extends SqlDataWrapper<DM>> extends SqlExecute<DM, DW> {

    /** 删除一条数据 */
    default int delete(Object obj) {
        DM entityTable = ((SqlDataHelper<DM, DW>) this).asEntityTable(obj);
        String sqlStr = entityTable.getDeleteSql(obj);
        Object id = entityTable.getDataColumnKey().getFieldValue(obj);
        return this.executeUpdate(sqlStr, id);
    }

    /** 删除一条数据 */
    default int delete(Class clazz, Object... args) {
        DM entityTable = ((SqlDataHelper<DM, DW>) this).asEntityTable(clazz);
        return delete(entityTable, args);
    }

    /** 删除一条数据 */
    default int delete(DM entityTable, Object... args) {
        String deleteSql = entityTable.getDeleteSql(null);
        return delete(entityTable, deleteSql, args);
    }

    /** 删除一条数据 */
    default int deleteWhere(Class clazz, String sqlWhere, Object... args) {
        DM entityTable = ((SqlDataHelper<DM, DW>) this).asEntityTable(clazz);
        return deleteWhere(entityTable, sqlWhere, args);
    }

    /** 删除一条数据 */
    default int deleteWhere(DM entityTable, String sqlWhere, Object... args) {
        String deleteSql = "DELETE FROM `" + entityTable.getTableName() + "` WHERE " + sqlWhere;
        return delete(entityTable, deleteSql, args);
    }

    /** 删除一条数据 */
    default int delete(DM entityTable, String deleteSql, Object... args) {
        int size = 0;
        if (entityTable.getSplitNumber() > 0) {
            for (int i = 0; i < entityTable.getSplitNumber(); i++) {
                String replace = entityTable.replaceTableName(deleteSql, entityTable.tableName(i));
                size = this.executeUpdate(replace, args);
            }
        } else {
            size = this.executeUpdate(deleteSql, args);
        }
        return size;
    }

    /** 批量删除数据 */
    default long deleteBatch(Class clazz, Collection<Object[]> paramList) {
        DM entityTable = ((SqlDataHelper<DM, DW>) this).asEntityTable(clazz);
        return deleteBatch(entityTable, paramList);
    }

    /** 批量删除数据 */
    default long deleteBatch(DM entityTable, Collection<Object[]> paramList) {
        String deleteSql = entityTable.getDeleteSql(null);
        return deleteBatch(entityTable, deleteSql, paramList);
    }

    /** 批量删除数据 */
    default long deleteBatch(DM entityTable, String deleteSql, Collection<Object[]> paramList) {
        long size = 0;
        if (entityTable.getSplitNumber() > 0) {
            for (int i = 0; i < entityTable.getSplitNumber(); i++) {
                String replace = entityTable.replaceTableName(deleteSql, entityTable.tableName(i));
                size = this.executeBatch(replace, paramList);
            }
        } else {
            size = this.executeBatch(deleteSql, paramList);
        }
        return size;
    }

    /** 清库 */
    default int truncates() {
        final Set<String> set = ((SqlDataHelper<DM, DW>) this).getDbTableStructMap().keySet();
        int size = 0;
        for (String tableName : set) {
            size += truncate(tableName);
        }
        return size;
    }

    /** 清空表 */
    default int truncate(Class<?> clazz) {
        DM entityTable = ((SqlDataHelper<DM, DW>) this).asEntityTable(clazz);
        return truncate(entityTable);
    }

    /** 清空表 */
    default int truncate(DM entityTable) {
        if (entityTable.getSplitNumber() > 0) {
            int size = 0;
            for (int i = 0; i < entityTable.getSplitNumber(); i++) {
                String sqlStr = "TRUNCATE TABLE `" + entityTable.getTableName() + "_" + i + "`";
                size += truncate(sqlStr);
            }
            return size;
        } else {
            String sqlStr = "TRUNCATE TABLE `" + entityTable.getTableName() + "`";
            return truncate(sqlStr);
        }
    }

    /** 清空表 */
    default int truncate(String tableName) {
        String sqlStr = "TRUNCATE TABLE `" + tableName + "`";
        return this.executeUpdate(sqlStr);
    }

}
