package org.wxd.boot.batis;


import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.wxd.boot.append.StreamWriter;
import org.wxd.boot.batis.struct.DbIndex;
import org.wxd.boot.str.StringUtil;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Objects;

/**
 * 数据集合模型映射关系
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-07-30 14:36
 **/
@Getter
@Setter
@Accessors(chain = true)
public class EntityTable implements Serializable, DbIndex {

    /** 数据模型类 */
    protected Class<?> entityClass;
    /** 表名 */
    protected String tableName;
    /** 表注释，备注 */
    protected String tableComment;
    /** 拆分表 */
    protected int splitNumber;
    protected int loadIndex = 9999;
    /** 主键 */
    protected EntityField dataColumnKey;
    /** 列名字 */
    protected LinkedHashMap<String, EntityField> columnMap = new LinkedHashMap<>();
    /** 所有数据 */
    protected LinkedList<LinkedHashMap<EntityField, Object>> rows = new LinkedList<>();

    public EntityTable() {
    }

    /**
     * 数据 表名
     *
     * @return
     */
    public String tableName(Object dbBase) {
        if (this.getSplitNumber() > 1) {
            int tableIndex;
            if (dbBase instanceof DbIndex) {
                tableIndex = ((DbIndex) dbBase).dbIndex(this);
            } else {
                tableIndex = this.dbIndex(dbBase, this);
            }
            return tableName(tableIndex);
        } else {
            return tableName;
        }
    }

    /**
     * 数据 表名
     *
     * @return
     */
    public String tableName(int tableIndex) {
        if (this.getSplitNumber() < 2) {
            return tableName;
        }
        if (tableIndex < 0 || tableIndex > getSplitNumber() - 1) {
            return tableName;
        }
        return tableName + "_" + tableIndex;
    }

    public String replaceTableName(String source, String newTableName) {
        return source.replace("`" + tableName + "`", "`" + newTableName + "`");
    }

    public int getSplitNumber() {
        if (splitNumber < 1) {
            splitNumber = 0;
        }
        return splitNumber;
    }

    /** 生成代码文件的名字 */
    @JSONField(serialize = true)
    public String getCodeClassName() {

        String[] split = tableName.split("_|-");
        if (split.length > 1) {
            for (int i = 1; i < split.length; i++) {
                split[i] = StringUtil.upperFirst(split[i]);
            }
        }
        String codeName = String.join("", split);

        return StringUtil.upperFirst(codeName);
    }

    /**
     * 数据模型描述
     * <p>
     * 表名字 + 类名 + atttable.tabledesc
     */
    public String getLogTableName() {
        return getTableName() + "(" + getEntityClass() + ", " + getTableComment() + ")";
    }

    public String toDataString() {
        return toDataString(50);
    }

    public String toDataString(int len) {
        StreamWriter append = new StreamWriter();
        DataWrapper.builderString(append, this, len);
        return append.toString();
    }

    /** 所有列 */
    public Collection<EntityField> getColumns() {
        return columnMap.values();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityTable dataMapping = (EntityTable) o;
        return Objects.equals(tableName, dataMapping.tableName);
    }

    @Override
    public int hashCode() {
        return tableName != null ? tableName.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "{clazz=" + entityClass + ", tableName='" + tableName + "', tableDesc='" + tableComment + "'}";
    }

}
