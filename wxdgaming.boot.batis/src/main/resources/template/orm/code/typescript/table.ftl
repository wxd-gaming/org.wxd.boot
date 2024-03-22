package ${packageName}.factory;


import lombok.Getter;
import wxdgaming.boot.batis.struct.DbBean;
import ${packageName}.bean.${codeClassName}Bean;

import java.io.Serializable;


/**
 * excel 构建 ${tableComment}
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: ${date}
 **/
@Getter
public class ${codeClassName}Factory extends DbBean<${codeClassName}Bean> implements Serializable {

    @Override public void initDb() {
        /*todo 实现一些数据分组*/

    }

}