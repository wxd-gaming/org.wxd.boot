package org.wxd.boot.batis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.wxd.agent.io.FileUtil;
import org.wxd.boot.lang.ObjectBase;
import org.wxd.boot.str.xml.XmlUtil;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-03-24 11:06
 **/
@Getter
@Setter
@Accessors(chain = true)
@Root
public class DbFactoryConfig extends ObjectBase implements Serializable {

    private static final long serialVersionUID = 1L;

    public static DbFactoryConfig build(String name) throws Exception {
        InputStream stream = FileUtil.findInputStream(name);
        return build(stream);
    }

    public static DbFactoryConfig build(InputStream stream) throws Exception {
        if (stream != null) {
            try {
                return XmlUtil.fromXml(stream, DbFactoryConfig.class);
            } finally {
                stream.close();
            }
        }
        return null;
    }

    @ElementList(inline = true)
    List<DbConfig> configs = new ArrayList<>();

}
