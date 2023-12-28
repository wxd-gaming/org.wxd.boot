package org.wxd.boot.httpclient.url;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.append.StreamWriter;
import org.wxd.boot.http.HttpHeadValueType;

import java.io.OutputStream;
import java.net.HttpURLConnection;

/**
 * 基于 HttpURLConnection 信息请求
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-01-05 12:55
 **/
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public abstract class Post<H extends Post> extends HttpBase<H> {

    protected Post(String uriPath) {
        super(uriPath);
        this.reqHttpMethod = "POST";
    }

    @Override
    protected void writer(HttpURLConnection urlConnection) throws Exception {
        try (StreamWriter streamWriter = new StreamWriter(512)) {
            writeTextParams(streamWriter);
            writesEnd(streamWriter);
            try (OutputStream outputStream = urlConnection.getOutputStream()) {
                outputStream.write(streamWriter.toBytes());
            }
        }
    }

    /** 普通字符串数据 */
    protected void writeTextParams(StreamWriter outWriter) throws Exception {

    }

    /** 添加结尾数据 */
    protected void writesEnd(StreamWriter outWriter) throws Exception {
        if (httpHeadValueType == HttpHeadValueType.Multipart) {
            outWriter.write("--").write(boundary).write("--").write("\r\n");
            outWriter.write("\r\n");
        }
    }

}
