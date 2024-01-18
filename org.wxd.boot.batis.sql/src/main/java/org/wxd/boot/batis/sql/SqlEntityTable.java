package org.wxd.boot.batis.sql;


import lombok.Getter;
import org.wxd.boot.batis.EntityTable;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-07-30 14:36
 **/
public class SqlEntityTable extends EntityTable implements Serializable {

    protected SqlDataWrapper dataBuilder;
    /*查询语句*/
    protected String selectSql = null;
    protected String selectSortSql = null;
    /*带查询条件的*/
    protected String selectWhereSql = null;
    protected String replaceSql = null;
    protected String deleteSql = null;
    /** 通过注解{@code Sql}获取的sql语句 */
    @Getter protected LinkedHashMap<String, String> sqls = new LinkedHashMap<>();

    public SqlEntityTable(SqlDataWrapper dataBuilder) {
        this.dataBuilder = dataBuilder;
    }

    protected String filterTable(String sqlStr, Object source) {
        if (source != null && this.getSplitNumber() > 0) {
            String newTableName = tableName(source);
            return replaceTableName(sqlStr, newTableName);
        }
        return sqlStr;
    }

    /**
     * 不包含 where 条件
     */
    public String getSelectSql() {
        if (selectSql == null) {
            dataBuilder.newSelectSql(this);
        }
        return selectSql;
    }

    /**
     * 不包含 where 条件
     */
    public String getSelectSortSql() {
        if (selectSortSql == null) {
            dataBuilder.newSelectSql(this);
        }
        return selectSortSql;
    }

    /**
     * 带主键的 where 查询语句
     */
    public String getSelectWhereSql() {
        if (selectWhereSql == null) {
            dataBuilder.newSelectSql(this);
        }
        return selectWhereSql;
    }

    /**
     * replace into 语句可以替代 insert和update
     */
    public String getReplaceSql(Object source) {
        if (replaceSql == null) {
            dataBuilder.newReplaceSql(this);
        }
        return filterTable(replaceSql, source);
    }

    /**
     * 根据主键数据删除表
     */
    public String getDeleteSql(Object source) {
        if (deleteSql == null) {
            dataBuilder.newDeleteSql(this);
        }
        return filterTable(deleteSql, source);
    }

}
