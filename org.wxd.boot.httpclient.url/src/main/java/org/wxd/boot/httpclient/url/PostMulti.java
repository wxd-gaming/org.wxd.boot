package org.wxd.boot.httpclient.url;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.append.StreamWriter;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.http.HttpDataAction;
import org.wxd.boot.str.StringUtil;

import java.util.Map;

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
public class PostMulti extends Post<PostMulti> {

    protected ObjMap reqMap = new ObjMap();

    protected PostMulti(String uriPath) {
        super(uriPath);
        this.reqHttpMethod = "POST";
    }

    public PostMulti putParams(Object key, Object value) {
        reqMap.put(key, value);
        return this;
    }

    public PostMulti putParams(Map map) {
        reqMap.putAll(map);
        return this;
    }

    protected void writeTextParams(StreamWriter outWriter) throws Exception {
        if (reqMap.isEmpty()) return;
        this.response.postText = HttpDataAction.httpDataEncoder(reqMap);
        if (StringUtil.notEmptyOrNull(this.response.postText)) {
            outWriter.write(this.response.postText);
            if (log.isDebugEnabled()) {
                log.debug("http send：" + this.response.postText);
            }
        }
    }

}
