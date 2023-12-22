package org.wxd.boot.net.controller.cmd;

import org.wxd.boot.agent.AgentService;
import org.wxd.boot.agent.LocalShell;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.io.FileWriteUtil;
import org.wxd.boot.agent.system.Base64Util;
import org.wxd.boot.agent.zip.ZipUtil;
import org.wxd.boot.lang.RunResult;
import org.wxd.boot.lang.SyncJson;
import org.wxd.boot.net.controller.ann.TextMapping;
import org.wxd.boot.net.web.hs.HttpSession;
import org.wxd.boot.str.json.FastJsonUtil;

import java.io.File;
import java.util.Map;

/**
 * 热更新
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-05-21 19:07
 **/
public interface HotLoad {

    final String sourceDir = "bin/hot_class";

    @TextMapping(remarks = "停止进程")
    default void stop(HttpSession httpSession) throws Exception {
        httpSession.responseText(SyncJson.ok().data("停止进程中 等待 30 秒"));
        LocalShell.exec(
                new File(System.getProperty("user.dir")),
                "sudo bash service.sh stop &"
        );
    }

    @TextMapping(remarks = "重启程序")
    default void restart(HttpSession httpSession) throws Exception {
        httpSession.responseText(SyncJson.ok().data("重启成功 等待 30 秒"));
        LocalShell.exec(
                new File(System.getProperty("user.dir")),
                "sudo bash service.sh restart &"
        );
    }

    /**
     * tmp_hot_class
     * 加载临时热更替文件
     */
    @TextMapping(remarks = "热更代码")
    default RunResult hotLoadClass(HttpSession httpSession) throws Throwable {
        String codebase64 = httpSession.getReqParams().getString("codebase64");
        byte[] decode = Base64Util.decode2Byte(codebase64);
        String javaClasses = ZipUtil.unzip2String(decode);

        httpSession.getReqParams().remove("codebase64");

        Map<String, byte[]> stringHashMap = FastJsonUtil.parseMap(javaClasses, String.class, byte[].class);

        FileWriteUtil.writeClassFile(sourceDir, stringHashMap);
        String hotClass = hotClass(httpSession);
        FileUtil.del(sourceDir);
        return RunResult.ok().setData(hotClass);
    }

    @TextMapping
    default String hotClass(HttpSession httpSession) throws Throwable {
        return AgentService.agentClass(sourceDir);
    }

}
