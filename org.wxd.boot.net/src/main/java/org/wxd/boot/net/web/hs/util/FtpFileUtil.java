package org.wxd.boot.net.web.hs.util;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.wxd.boot.agent.io.TemplatePack;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.collection.OfList;
import org.wxd.boot.format.ByteFormat;
import org.wxd.boot.http.HttpHeadValueType;
import org.wxd.boot.http.HttpDataAction;
import org.wxd.boot.net.web.hs.HttpSession;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.JvmUtil;
import org.wxd.boot.timer.MyClock;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * fpt文件管理
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-07-02 22:24
 **/
public class FtpFileUtil implements Serializable {

    public static String ftpFile(HttpSession httpSession, String ftpUrl, String ftpPath, String search, int pageSize, int pageNumber) {

        String userHome = JvmUtil.userHome();

        if (!ftpUrl.startsWith("/")) {
            ftpUrl = "/" + ftpUrl;
        }

        if (pageSize <= 0) pageSize = 50;
        if (pageNumber < 1) pageNumber = 1;
        if (StringUtil.emptyOrNull(search)) search = "";
        if (StringUtil.emptyOrNull(ftpPath) || !ftpPath.startsWith(userHome)) {
            ftpPath = userHome;
        }

        final File file = new File(ftpPath);
        final File[] files = file.listFiles();

        if (ftpPath.indexOf(":") > 0) {
            ftpPath = ftpPath.substring(ftpPath.indexOf(":") + 1);
        }

        ftpPath = ftpPath.replace("\\", "/");

        if ("/".equals(ftpPath)) {
            ftpPath = "";

        }
        while (ftpPath.startsWith("/")) {
            if (ftpPath.length() < 2) break;
            ftpPath = ftpPath.substring(1);
        }
        ObjMap objMap = new ObjMap();
        List<String> pathList = OfList.asList(ftpPath.split("\\/"));
        pathList.add(0, "/");
        String htmlPath = "";
        ArrayList<ObjMap> urls = new ArrayList<>();

        objMap.append("urls", urls);

        for (String s : pathList) {
            if (htmlPath.length() > 1) htmlPath += "/";
            htmlPath += s;
            urls.add(new ObjMap().append("text", s).append("url", ftpUrl + "?path=" + HttpDataAction.urlEncoder(htmlPath) + "&pageSize=" + pageSize));
        }

        int pageMaxNumber = 0;

        List<ObjMap> datas = new ArrayList<>();
        if (files != null && files.length > 0) {
            pageMaxNumber = (files.length / pageSize) + (files.length % pageSize > 0 ? 1 : 0);
            if (pageNumber >= pageMaxNumber) {
                pageNumber = pageMaxNumber;
            }

            int pageIndex = pageNumber - 1;
            if (pageIndex < 0) {
                pageIndex = 0;
            }
            int skip = pageSize * pageIndex;
            final String searchFile = search.toLowerCase();
            List<File> collect = Arrays.stream(files)
                    .filter(file1 -> {
                        if (StringUtil.notEmptyOrNull(searchFile)) {
                            if (!file1.getName().toLowerCase().contains(searchFile)) {
                                /*如果有搜索，不匹配不要*/
                                return false;
                            }
                        }
                        return true;
                    })
                    .sorted((o1, o2) -> o2.getName().compareToIgnoreCase(o1.getName()))
                    .skip(skip)
                    .limit(pageSize)
                    .collect(Collectors.toList());

            int count = skip;

            for (File list : collect) {
                String listPath = list.getPath();

                final String dateString = MyClock.formatDate(MyClock.SDF_YYYYMMDDHHMMSS_2, list.lastModified());

                String byteString;

                if (list.isFile()) {
                    final ByteFormat byteFormat = new ByteFormat().addFlow(list.length());
                    byteString = byteFormat.toString(ByteFormat.FormatInfo.All);
                } else {
                    byteString = "目录";
                }

                count++;
                datas.add(new ObjMap()
                        .append("id", String.valueOf(count))
                        .append("text", list.getName())
                        .append("url", ftpUrl + "?path=" + HttpDataAction.urlEncoder(listPath) + "&pageSize=" + pageSize)
                        .append("isFile", list.isFile())
                        .append("dateString", dateString)
                        .append("byteString", byteString)
                );
            }
        }

        objMap.append("datas", datas);

        objMap.append("search", search);
        objMap.append("pageSize", String.valueOf(pageSize));
        objMap.append("pageNumber", String.valueOf(pageNumber));
        objMap.append("pageMaxNumber", String.valueOf(pageMaxNumber));
        objMap.append("pageMaxIndex", String.valueOf(pageMaxNumber - 1));
        int pageUpIndex = pageNumber - 1;
        if (pageUpIndex < 1) {
            pageUpIndex = 1;
        }
        objMap.append("pageUpIndex", String.valueOf(pageUpIndex));

        int pageNextIndex = pageNumber + 1;
        if (pageNextIndex >= pageMaxNumber) {
            pageNextIndex = pageMaxNumber;
        }
        objMap.append("pageNextIndex", String.valueOf(pageNextIndex));

        TemplatePack templatePack = TemplatePack.build(FtpFileUtil.class.getClassLoader(), "template/ftp");
        String ftphtml = templatePack.ftl2String("ftp.ftl", objMap);

        if (httpSession != null) {
            if (httpSession.getReqParams().getIntValue("pageSize") != pageSize
                    || httpSession.getReqParams().getIntValue("pageNumber") != pageNumber) {
                String uriPath = httpSession.getUriPath();
                httpSession.getReqParams().put("pageSize", pageSize);
                httpSession.getReqParams().put("pageNumber", pageNumber);
                String httpData = HttpDataAction.httpDataEncoder(httpSession.getReqParams());
                httpSession.getResHeaderMap().put(HttpHeaderNames.LOCATION.toString(), uriPath + "?" + httpData);
                httpSession.response(HttpVersion.HTTP_1_1, HttpResponseStatus.TEMPORARY_REDIRECT, HttpHeadValueType.Html, ftphtml.getBytes(StandardCharsets.UTF_8));
            } else {
                httpSession.response(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, HttpHeadValueType.Html, ftphtml.getBytes(StandardCharsets.UTF_8));
            }
        }

        return ftphtml;
    }

}
