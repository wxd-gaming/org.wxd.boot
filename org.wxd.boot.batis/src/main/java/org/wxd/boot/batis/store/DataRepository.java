package org.wxd.boot.batis.store;

import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.system.ReflectContext;
import org.wxd.boot.batis.DataWrapper;
import org.wxd.boot.batis.EntityTable;
import org.wxd.boot.batis.struct.DbBean;
import org.wxd.boot.str.StringUtil;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 数据仓储
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-04-19 15:00
 **/
@Slf4j
public abstract class DataRepository<DM extends EntityTable, DW extends DataWrapper<DM>> implements Serializable {

    protected ConcurrentMap<String, DbBean> beanMap = new ConcurrentHashMap<>();

    /** 获取编译器 */
    public abstract DW dataBuilder();

    /** 数据来源名字 */
    public abstract String dataName();

    public <R extends DbBean> R getDbBean(Class<R> beanClazz) {
        Class<?> tClass = ReflectContext.getTClass(beanClazz);
        DM entityTable = dataBuilder().asEntityTable(tClass);
        return getDbBean(beanClazz, entityTable);
    }

    public <R extends DbBean> R getDbBean(Class<R> beanClazz, DM entityTable) {
        DbBean dbBean = beanMap.computeIfAbsent(entityTable.getTableName(), l -> load(beanClazz, entityTable));
        return (R) dbBean;
    }

    public void removeAll() {
        beanMap = new ConcurrentHashMap<>();
    }

    public void removeDbBean(Class<? extends DbBean> beanClazz) {
        Class<?> tClass = ReflectContext.getTClass(beanClazz);
        DM entityTable = dataBuilder().asEntityTable(tClass);
        removeDbBean(entityTable.getTableName());
    }

    /**
     * 是否包含
     */
    public boolean hasDbBean(Class<? extends DbBean> beanClazz) {
        Class<?> tClass = ReflectContext.getTClass(beanClazz);
        DM entityTable = dataBuilder().asEntityTable(tClass);
        return hasDbBean(entityTable.getTableName());
    }

    /**
     * 是否包含
     */
    public boolean hasDbBean(String dbName) {
        return beanMap.containsKey(dbName);
    }

    public void removeDbBean(String name) {
        beanMap.remove(name);
    }

    public void clearBean() {
        beanMap.clear();
    }

    /**
     * 加载
     *
     * @param forceLoad 强制加载
     * @param packages  需要加载的包名
     */
    public void load(boolean forceLoad, ClassLoader classLoader, String... packages) {
        StringBuilder stringBuilder = new StringBuilder();

        List<Class<?>> list = ReflectContext.Builder
                .of(classLoader, packages).build()
                .classWithSuper(DbBean.class)
                .collect(Collectors.toList());
        load(stringBuilder, forceLoad, list);
        log.info(stringBuilder.toString());
    }

    /**
     * 加载
     *
     * @param forceLoad   强制加载
     * @param beanClasses 需要加载的类
     */
    public void load(boolean forceLoad, Collection<Class<?>> beanClasses) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.setLength(0);
        load(stringBuilder, forceLoad, beanClasses);
        log.info(stringBuilder.toString());
    }

    /**
     * @param stringAppend
     * @param forceLoad    强制重新加载
     * @param beanClasses
     */
    public void load(StringBuilder stringAppend, boolean forceLoad, Collection<Class<?>> beanClasses) {
        writerTitle(stringAppend, 1);
        for (Class<?> clazz : beanClasses) {
            if (!DbBean.class.isAssignableFrom(clazz)) {
                /*如果不是dbbean*/
                continue;
            }
            Class<? extends DbBean> beanClazz = (Class<? extends DbBean>) clazz;
            Class<?> tClass = ReflectContext.getTClass(beanClazz);
            DM entityTable = this.dataBuilder().asEntityTable(tClass);
            DbBean bean;
            if (forceLoad) {
                bean = load(beanClazz, entityTable);
            } else {
                bean = getDbBean(beanClazz, entityTable);
            }
            addReloadMsg(stringAppend, entityTable, bean);
        }
        writerTitle(stringAppend, 2);
    }

    public <R extends DbBean> R load(Class<R> beanClazz, DM entityTable) {
        try {
            R dbBean = beanClazz.getDeclaredConstructor().newInstance();
            dbBean.setDataStruct(entityTable);
            dbBean.setModelList(readDbList(entityTable));
            dbBean.initDb();
            return dbBean;
        } catch (Exception e) {
            throw new Throw("实体类：" + beanClazz.getName() + ", " + e.getMessage(), e);
        }
    }

    public abstract List readDbList(DM entityTable) throws Exception;

    protected void addReloadMsg(StringBuilder out, DM entityTable, DbBean dbBean) {
        if (out != null) {
            out
                    .append("|").append(StringUtil.padRight(String.valueOf(dbBean.dbSize()), 10, ' ')).append("\t")
                    .append("|").append(StringUtil.padRight(entityTable.getTableName(), 40, ' ')).append("\t")
                    .append("|").append(StringUtil.padRight(entityTable.getEntityClass().getSimpleName(), 40, ' ')).append("\t")
                    .append("|").append(StringUtil.padRight(entityTable.getTableComment(), 60, ' ')).append("\t")
                    .append("\n");
        }
    }

    protected void writerTitle(StringBuilder out, int type) {
        out.append("\n");
        if (type == 1) {
            out.append("数据库：").append(dataName()).append("\n");
        }

        out
                .append("|").append(StringUtil.padRight("读取数量", 10, ' ')).append("\t")
                .append("|").append(StringUtil.padRight("映射表名", 40, ' ')).append("\t")
                .append("|").append(StringUtil.padRight("映射类名", 40, ' ')).append("\t")
                .append("|").append(StringUtil.padRight("映射备注", 60, ' ')).append("\t")
                .append("\n");

        if (type == 2) {
            out.append("数据库：").append(dataName()).append("\n");
        } else {
            out.append("\n");
        }
    }

}
