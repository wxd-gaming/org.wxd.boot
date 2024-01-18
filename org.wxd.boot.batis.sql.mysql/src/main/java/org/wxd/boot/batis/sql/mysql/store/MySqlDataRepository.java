package org.wxd.boot.batis.sql.mysql.store;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.wxd.boot.batis.sql.SqlDataWrapper;
import org.wxd.boot.batis.sql.SqlEntityTable;
import org.wxd.boot.batis.sql.mysql.MysqlDataHelper;
import org.wxd.boot.batis.store.DataRepository;

import java.util.List;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-04-19 17:51
 **/
@Getter
@Setter
@Accessors(chain = true)
public class MySqlDataRepository extends DataRepository<SqlEntityTable, SqlDataWrapper<SqlEntityTable>> {

    protected MysqlDataHelper mysqlDao;

    public MySqlDataRepository() {
    }

    @Override
    public SqlDataWrapper<SqlEntityTable> dataBuilder() {
        return mysqlDao.getDataWrapper();
    }

    @Override
    public String dataName() {
        return mysqlDao.getDbBase();
    }

    @Override
    public final List readDbList(SqlEntityTable entityTable) throws Exception {
        return mysqlDao.queryEntities(entityTable, entityTable.getSelectSortSql());
    }


}
