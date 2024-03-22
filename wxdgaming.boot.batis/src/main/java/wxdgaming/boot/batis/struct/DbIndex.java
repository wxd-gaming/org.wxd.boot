package wxdgaming.boot.batis.struct;

import wxdgaming.boot.batis.EntityField;
import wxdgaming.boot.batis.EntityTable;
import wxdgaming.boot.core.str.StringUtil;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-10-21 13:47
 **/
public interface DbIndex {

    default int dbIndex(EntityTable entityTable) {
        return dbIndex(this, entityTable);
    }

    default int dbIndex(Object source, EntityTable entityTable) {
        int splitNumber = entityTable.getSplitNumber();
        EntityField dataColumnKey = entityTable.getDataColumnKey();
        Object fieldValue = dataColumnKey.getFieldValue(source);
        long hashcode;
        if (fieldValue instanceof Integer) {
            hashcode = (Integer) fieldValue;
        } else if (fieldValue instanceof Long) {
            hashcode = (Long) fieldValue;
        } else {
            hashcode = StringUtil.hashcode(fieldValue.toString(), true);
        }
        /*采用双重冗余 hash 捅 分配数据 插槽 3.8 这个因子不能改*/
        int index = StringUtil.hashIndex(hashcode, splitNumber);
        return index;
    }

}