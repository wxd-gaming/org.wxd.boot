package org.wxd.boot.net.web.hs.controller.cmd;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.lang.SyncJson;
import org.wxd.boot.net.auth.Sign;
import org.wxd.boot.net.controller.ann.TextMapping;
import org.wxd.boot.net.web.hs.HttpSession;

/**
 * http 登录验证
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-05-21 19:36
 **/
public interface HttpSign extends Sign<HttpSession> {

    /** 用于http登录生成秘钥的，秘钥过期时间是10分钟 */
    @Override
    @TextMapping(url = "/", remarks = "登录")
    default SyncJson sign(HttpSession session, ObjMap putData) {
        SyncJson runResult = Sign.super.sign(session, putData);
        if (runResult != null && runResult.code() == 1) {
            session.getResCookie().addCookie(
                    HttpHeaderNames.AUTHORIZATION,
                    runResult.getString(HttpHeaderNames.AUTHORIZATION.toString()),
                    "/"
            );
        }
        return runResult;
    }

}
