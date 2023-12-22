package org.wxd.boot.starter.net.controller;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.loader.JavaCoderCompile;
import org.wxd.boot.agent.system.Base64Util;
import org.wxd.boot.agent.zip.ZipUtil;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.httpclient.url.HttpBuilder;
import org.wxd.boot.lang.RunResult;
import org.wxd.boot.net.auth.IAuth;
import org.wxd.boot.net.auth.SignConfig;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.str.json.FastJsonUtil;
import org.wxd.boot.system.PrintConsole;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * 执行java源码
 *
 * @author: Troy.Chen(失足程序员, 15388152619)
 * @version: 2021-08-10 14:24
 **/
@Slf4j
public class PostCode implements PrintConsole {

    public static final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

    public PostCode() {
    }

    protected String readIp() {
        String readLine = null;
        try {
            int i = 0;
            final Post_Run_Config post_run_config = Post_Run_Config.get();

            final int maxLen = post_run_config.getUrlList().stream().mapToInt(v -> v.getName().length()).max().orElse(10) + 16;

            for (Post_Run_Config.Urls urls : post_run_config.getUrlList()) {
                i++;
                System.out.print(StringUtil.padRight(urls.getServerId() + " - " + urls.getName(), maxLen, ' ') + "\t");
                if (i % 8 == 0) {
                    System.out.println();
                }
            }

            System.out.println();
            System.out.print("请选择服务器 ip 输入 数字：");

            readLine = bufferedReader.readLine();
            System.out.println();
            int sid = Integer.parseInt(readLine);
            Post_Run_Config.Urls urls = post_run_config.findBySid(sid);
            if (StringUtil.notEmptyOrNull(urls.getPwd())) {
                System.out.println("！！！！！！谨慎操作远程服务器请！！！！！");
                System.out.print("输入密码：");
                String pwd = bufferedReader.readLine();
                System.out.println();
                if (urls.getPwd().equalsIgnoreCase(pwd)) {
                    System.out.println("===========!!!!!!密码错误!!!!!!====================");
                    System.out.println();
                    return readIp();
                }
            }
            String selectIp = urls.getUrl();
            System.out.println("选择服务器：" + urls.getName() + ", " + urls.getUrl());
            if (StringUtil.notEmptyOrNull(urls.getPwd())) {
                System.out.println("！！！！！！再次确认选择的服务器是否正确！！！！！");
                bufferedReader.readLine();
            }
            return selectIp;
        } catch (Exception e) {
            System.out.println("输入错误：" + readLine + " 请重新执行");
            return readIp();
        }
    }


    /**
     * 默认读取的是test目录
     *
     * @param className
     * @return
     */
    protected String codeTestPath(String className) {
        String userDir = System.getProperty("user.dir");
        userDir += "/src/test/java";
        return codeFilePath(userDir, className);
    }

    protected String codeMainPath(String className) {
        String userDir = System.getProperty("user.dir");
        userDir += "/src/main/java";
        return codeFilePath(userDir, className);
    }

    protected String codeFilePath(String path, String className) {
        String filePath = path + "/" + className.replace(".", "/") + ".java";
        return filePath;
    }

    public RunResult postCode(String url, String codePath, Object params) {
        try {
//            String codeString = FileReadUtil.readString(codePath, StandardCharsets.UTF_8);
            Map<String, byte[]> stringMap = new JavaCoderCompile().compilerJava(codePath).toBytesMap();
            String jsonString = FastJsonUtil.toJson(stringMap);
            byte[] compress = ZipUtil.zip2Bytes(jsonString);
            String encodeToString = Base64Util.encode2String(compress);

            ObjMap postParams = new ObjMap();
            postParams.put("codebase64", encodeToString);
            if (params != null) {
                postParams.put("params", params);
            }
            return post0(url, postParams);
        } catch (Throwable throwable) {
            log.error("动态执行代码", throwable);
        }
        return null;
    }

    protected RunResult post0(String url, Map<Object, Object> objects) {
        String readIp = readIp();
        objects.put(HttpHeaderNames.AUTHORIZATION.toString(), SignConfig.get().optByUser("root").map(IAuth::getToken).orElse(""));
        String post = HttpBuilder.postMulti(readIp + "/" + url)
                .timeout(400000)
                .header(HttpHeaderNames.AUTHORIZATION.toString(), SignConfig.get().optByUser("root").map(IAuth::getToken).orElse(""))
                .header(HttpHeaderNames.CONTENT_ENCODING.toString(), HttpHeaderValues.GZIP.toString())
                // .addCookie(HttpHeaderNames.AUTHORIZATION, SignConfig.get().getToken())
                .putParams(objects)
                .request()
                .bodyString();

        post = post.replace("<br>", "\n");
        log.info(post);
        RunResult r = null;
        try {
            r = FastJsonUtil.parse(post, RunResult.class);
            if (r.getCode() != 0) {
                log.warn("\n" + r.getCode() + " - " + r.getMsg());
            } else {
                if (r.getData() instanceof String) {
                    log.warn("\n" + r.getData());
                } else {
                    log.warn("\n" + FastJsonUtil.toJsonFmt(r.getData()));
                }
            }
        } catch (Exception e) {
            log.error("\n" + post, e);
        }
        return r;
    }

}
