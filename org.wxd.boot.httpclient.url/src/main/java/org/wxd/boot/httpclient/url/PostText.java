package org.wxd.boot.httpclient.url;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.append.StreamWriter;
import org.wxd.boot.http.HttpHeadValueType;
import org.wxd.boot.str.StringUtil;

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
public class PostText extends Post<PostText> {

    protected PostText(String uriPath) {
        super(uriPath);
        this.reqHttpMethod = "POST";
    }

    /** 设置提交的参数 */
    public PostText paramText(String params) {
        this.response.postText = params;
        return this;
    }

    /** 设置提交的参数 */
    public PostText paramJson(String json) {
        this.response.postText = json;
        this.httpHeadValueType = HttpHeadValueType.Json;
        return this;
    }

    /** 设置提交的参数 */
    public PostText paramXJson(String json) {
        this.response.postText = json;
        this.httpHeadValueType = HttpHeadValueType.XJson;
        return this;
    }

    @Override protected void writeTextParams(StreamWriter outWriter) throws Exception {
        if (StringUtil.notEmptyOrNull(this.response.postText)) {
            outWriter.write(this.response.postText);
            if (log.isDebugEnabled()) {
                log.debug("http send：" + this.response.postText);
            }
        }
    }

}
