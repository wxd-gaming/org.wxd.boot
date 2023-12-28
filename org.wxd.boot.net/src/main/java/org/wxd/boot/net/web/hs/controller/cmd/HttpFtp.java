package org.wxd.boot.net.web.hs.controller.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wxd.boot.agent.io.FileReadUtil;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.http.HttpDataAction;
import org.wxd.boot.net.controller.ann.TextMapping;
import org.wxd.boot.net.web.hs.HttpServer;
import org.wxd.boot.net.web.hs.HttpSession;
import org.wxd.boot.net.web.hs.util.FtpFileUtil;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.JvmUtil;

import java.io.File;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-07-02 22:28
 **/
public interface HttpFtp {

    Logger log = LoggerFactory.getLogger(HttpFtp.class);

    @TextMapping
    default void ftp(HttpSession httpSession, ObjMap putData) throws Exception {

        String userHome = JvmUtil.userHome();
        userHome = FileUtil.getCanonicalPath(userHome);

        boolean show = putData.getBooleanValue("show");

        int pageSize = putData.getIntValue("pageSize");
        int pageNumber = putData.getIntValue("pageNumber");
        String search = putData.getString("search");

        String path = putData.getString("path");
        if (StringUtil.emptyOrNull(path)) {
            path = userHome;
        }

        path = HttpDataAction.urlDecoder(path);
        path = FileUtil.getCanonicalPath(path);
        if (!path.startsWith(userHome)) {
            path = userHome;
        }

        final File file = new File(path);
        if (file.isFile()) {
            if (show) {
                httpSession.responseText(FileReadUtil.readBytes(file));
            } else {
                HttpServer.downloadFile(httpSession, file);
            }
        } else {
            FtpFileUtil.ftpFile(httpSession, httpSession.getUriPath(), path, search, pageSize, pageNumber);
        }
    }
}
