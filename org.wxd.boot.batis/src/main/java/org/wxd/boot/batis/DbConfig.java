package org.wxd.boot.batis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.lang.Record2;
import org.wxd.boot.lang.ObjectBase;
import org.wxd.boot.str.xml.XmlUtil;

import java.io.InputStream;
import java.io.Serializable;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
@Getter
@Setter
@Accessors(chain = true)
public class DbConfig extends ObjectBase implements Serializable, Cloneable {

    /** 先从外部配置config 目录读取 然后再从resources 读取 */
    public static DbConfig loadConfigXml(String fileName) throws Exception {
        Record2<String, InputStream> inputStream = FileUtil.findInputStream(fileName);
        if (inputStream == null) {
            throw new RuntimeException("文件不存在 " + fileName);
        }
        return loadConfigXml(inputStream.t2());
    }

    /** 先从外部配置config 目录读取 然后再从resources 读取 */
    public static DbConfig loadConfigXml(InputStream stream) throws Exception {
        if (stream != null) {
            try {
                return XmlUtil.fromXml(stream, DbConfig.class);
            } finally {
                stream.close();
            }
        }
        return null;
    }

    /** 命名 */
    private String name;
    /** 链接池 */
    private boolean show_sql = false;
    /** 链接池 */
    private boolean connectionPool = false;
    /** 创建数据库 */
    private boolean createDbBase = true;
    /** 批处理线程数 */
    private int batchSizeThread;
    /** 数据库ip */
    private String dbHost;
    /** 数据库端口 */
    private int dbPort;
    /** 数据库名字 */
    private String dbBase;
    /** 用户 */
    private String dbUser;
    /** 密码 */
    private String dbPwd;

    public DbConfig clone(String dbName) throws CloneNotSupportedException {
        return clone().setDbBase(dbName);
    }

    @Override
    public DbConfig clone() throws CloneNotSupportedException {
        return (DbConfig) super.clone();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("DbConfig{");
        sb.append("name='").append(name).append('\'');
        sb.append(", show_sql=").append(show_sql);
        sb.append(", connectionPool=").append(connectionPool);
        sb.append(", batchSizeThread=").append(batchSizeThread);
        sb.append(", dbHost=").append(dbHost).append(":").append(dbPort).append("/").append(dbBase);
        sb.append(", dbUser='").append(dbUser).append('\'');
        sb.append('}');
        return sb.toString();
    }
//    @Override
//    public String toString() {
//        return new StringJoiner(", ", DbConfig.class.getSimpleName() + "[", "]")
//                .add("name='" + name + "'")
//                .add("show_sql=" + show_sql)
//                .add("connectionPool=" + connectionPool)
//                .add("batchSizeThread=" + batchSizeThread)
//                .add("dbHost=" + dbHost + ":" + dbPort + "/" + dbBase)
//                .add("dbUser='" + dbUser + "'")
//                .toString();
//    }

}
