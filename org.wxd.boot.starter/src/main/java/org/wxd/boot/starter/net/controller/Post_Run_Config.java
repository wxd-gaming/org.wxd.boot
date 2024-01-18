package org.wxd.boot.starter.net.controller;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.io.FileWriteUtil;
import org.wxd.boot.agent.lang.Record2;
import org.wxd.boot.lang.ObjectBase;
import org.wxd.boot.str.xml.XmlUtil;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-03-11 18:07
 **/
@Slf4j
@Root
public class Post_Run_Config extends ObjectBase implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Post_Run_Config instance = null;

    static {
        try {
            instance = postRunConfig();
        } catch (Exception e) {
            log.error("读取配置数据", e);
        }
        if (instance == null) {
            try {
                instance = new Post_Run_Config();
                instance.getUrlList().add(new Urls(6001, "本机", "http://127.0.0.1:18611"));
                String fileName = "config/post-run-config.xml";
                FileWriteUtil.writeString(fileName, instance.toXml());
                System.out.println("初始化配置：" + fileName);
            } catch (Exception e) {
                log.error("初始化配置", e);
            }
        }
    }

    public static Post_Run_Config get() {
        return instance;
    }

    public static Post_Run_Config postRunConfig() throws Exception {
        Record2<String, InputStream> inputStream = FileUtil.findInputStream("post-run-config.xml");
        if (inputStream == null) {
            return null;
        }
        System.out.println("读取文件目录：" + inputStream.t1());
        return postRunConfig(inputStream.t2());
    }

    public static Post_Run_Config postRunConfig(InputStream stream) throws Exception {
        if (stream != null) {
            try {
                return XmlUtil.fromXml(stream, Post_Run_Config.class);
            } finally {
                stream.close();
            }
        }
        return null;
    }

    @ElementList(inline = true)
    private List<Urls> urlList = new ArrayList<>();

    public Urls findBySid(int sid) {
        return urlList.stream().filter(v -> v.getServerId() == sid).findAny().orElse(null);
    }

    public List<Urls> getUrlList() {
        return urlList;
    }

    public Post_Run_Config setUrlList(List<Urls> urlList) {
        this.urlList = urlList;
        return this;
    }

    @Root
    @Getter
    @Setter
    @Accessors(chain = true)
    @NoArgsConstructor
    public static class Urls {

        @Attribute
        private int serverId = 0;
        @Attribute
        private String name = "";
        @Attribute
        private String url = "";
        @Attribute(required = false)
        private String pwd = "";

        public Urls(int serverId, String name, String url) {
            this.serverId = serverId;
            this.name = name;
            this.url = url;
        }

        public Urls(int serverId, String name, String url, String pwd) {
            this.serverId = serverId;
            this.name = name;
            this.url = url;
            this.pwd = pwd;
        }
    }

}
