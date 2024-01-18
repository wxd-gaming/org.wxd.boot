package org.wxd.boot.agent.io;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * ftl 模板
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-12-19 21:38
 **/
@Slf4j
public class TemplatePack {

    public static TemplatePack build(String dir) {
        return build(Thread.currentThread().getContextClassLoader(), dir);
    }

    public static TemplatePack build(ClassLoader classLoader, String dir) {
        TemplatePack templatePack = new TemplatePack(classLoader, dir);
        return templatePack;
    }

    protected static Configuration newTemplatePack() {
        //1.实例化模板对象
        Configuration configuration = new Configuration();
        //2.设置模板的上下文 和 设计加载模板路径（模板存放路径）
        //3设计模板的编码格式
        configuration.setDefaultEncoding("UTF-8");
        return configuration;
    }

    protected final String baseDir;
    private final Configuration classLoaderTemplatePack;

    private final Configuration dirTemplatePack;

    protected TemplatePack(ClassLoader classLoader, String dir) {
        this.baseDir = dir;
        if (classLoader.getResource(dir) != null) {
            /*jar包内的资源获取*/
            this.classLoaderTemplatePack = newTemplatePack();
            this.classLoaderTemplatePack.setClassLoaderForTemplateLoading(classLoader, dir);
        } else {
            classLoaderTemplatePack = null;
        }

        if (FileUtil.exists(dir)) {
            /*非jar包内资源*/
            this.dirTemplatePack = newTemplatePack();
            try {
                this.dirTemplatePack.setDirectoryForTemplateLoading(new File(dir));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.dirTemplatePack = null;
        }
    }

    public Template getTemplate(String ftlName) throws IOException {
        //4.加载模板文件，获取模板对象
        String fileName = baseDir + File.separator + ftlName;
        if (FileUtil.exists(fileName)) {
            log.debug("读取文件目录：{}", fileName);
            return this.dirTemplatePack.getTemplate(ftlName);
        } else {
            log.debug("读取jar包资源：{}", fileName);
            return this.classLoaderTemplatePack.getTemplate(ftlName);
        }
    }

    public String ftl2String(String ftlName, Map dataObj) {
        return new String(ftl2Bytes(ftlName, dataObj), StandardCharsets.UTF_8);
    }

    public void ftl2File(String ftlName, Map dataObj, String outFile) {
        FileWriteUtil.writeBytes(outFile, ftl2Bytes(ftlName, dataObj));
    }

    /** linux shell 文件 */
    public void ftl2Shell(String ftlName, Map dataObj, String outFile) {
        String content = ftl2String(ftlName, dataObj);
        content = content.replace("\\r", "");
        FileWriteUtil.writeString(outFile, content);
    }

    public byte[] ftl2Bytes(String ftlName, Map dataObj) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ftl2OutputStream(ftlName, dataObj, outputStream);
            return outputStream.toByteArray();
        } catch (Throwable throwable) {
            throw Throw.as(throwable);
        }
    }

    public void ftl2OutputStream(String ftl, Map dataObj, OutputStream outputStream) {
        try {
            //1.实例化模板对象
            Template template = getTemplate(ftl);
            try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                template.process(dataObj, outputStreamWriter);
            }
        } catch (Throwable throwable) {
            throw Throw.as(throwable);
        }
    }

}
