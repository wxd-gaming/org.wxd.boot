package org.wxd.boot.httpclient;

import lombok.Getter;
import org.wxd.boot.agent.io.FileUtil;

import java.io.File;

/**
 * http 协议类型
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
@Getter
public enum HttpHeadValueType {
    /**  */
    All("*/*; charset=UTF-8"),
    Gzip("gzip"),
    Close("close"),
    /** octet-stream */
    OctetStream("application/octet-stream; charset=UTF-8"),

    /** application/x-www-form-urlencoded; charset=UTF-8 */
    Application("application/x-www-form-urlencoded; charset=UTF-8"),

    /** text/plain; charset=UTF-8 */
    Text("text/plain; charset=UTF-8"),

    /** applicaton/x-json; charset=UTF-8 */
    XJson("applicaton/x-json; charset=UTF-8"),

    /** application/json; charset=UTF-8 */
    Json("application/json; charset=UTF-8"),

    /** text/html; charset=UTF-8 */
    Html("text/html; charset=UTF-8"),

    /** text/xml; charset=UTF-8 */
    Xml("text/xml; charset=UTF-8"),

    /** text/javascript; charset=UTF-8 */
    Javascript("text/javascript; charset=UTF-8"),

    /** text/css; charset=UTF-8 */
    CSS("text/css; charset=UTF-8"),

    /** multipart/form-data; charset=UTF-8 */
    Multipart("multipart/form-data; charset=UTF-8"),

    /** image/x-ico */
    ICO("image/x-ico"),

    /** image/x-icon */
    ICON("image/x-icon"),

    /** image/gif */
    GIF("image/gif"),

    /** image/jpeg */
    JPG("image/jpeg"),

    /** image/png */
    PNG("image/png"),
    ;

    final String value;

    HttpHeadValueType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * 获取文件的上传类型，图片格式为image/png,image/jpg等。非图片为application/octet-stream
     *
     * @param f
     * @return
     * @throws Exception
     */
    public static HttpHeadValueType findContentType(File f) {
        String fileName = f.getName();
        return findContentType(fileName);
    }

    public static HttpHeadValueType findContentType(String fileName) {
        String extendName = FileUtil.extendName(fileName);
        return switch (extendName) {
            case "htm", "html", "jsp", "asp", "aspx", "xhtml" -> HttpHeadValueType.Html;
            case "css" -> HttpHeadValueType.CSS;
            case "js" -> HttpHeadValueType.Javascript;
            case "xml" -> HttpHeadValueType.Xml;
            case "json" -> HttpHeadValueType.Json;
            case "xjson" -> HttpHeadValueType.XJson;
            case "ico" -> HttpHeadValueType.ICO;
            case "icon" -> HttpHeadValueType.ICON;
            case "gif" -> HttpHeadValueType.GIF;
            case "jpg", "jpe", "jpeg" -> HttpHeadValueType.JPG;
            case "png" -> HttpHeadValueType.PNG;
            default -> HttpHeadValueType.OctetStream;
        };
    }


}

