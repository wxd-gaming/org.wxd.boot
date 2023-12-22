package org.wxd.boot.net.web.hs.controller.cmd;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.net.auth.AuthModule;
import org.wxd.boot.net.auth.SignCheck;
import org.wxd.boot.net.web.hs.HttpSession;
import org.wxd.boot.str.StringUtil;

import java.lang.reflect.Method;

/**
 * http 登录验证
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-05-21 19:36
 **/
public interface HttpSignCheck extends SignCheck<HttpSession> {

    static String checkAuth(Method cmdMethod, HttpSession session, ObjMap putData) {
        String s = AuthModule.checkToken(cmdMethod, session, putData.getString(HttpHeaderNames.AUTHORIZATION.toString()));
        if (StringUtil.notEmptyOrNull(s)) {
            s = AuthModule.checkToken(cmdMethod, session, session.getReqCookies().findCookieValue(HttpHeaderNames.AUTHORIZATION));
        }
        if (StringUtil.notEmptyOrNull(s)) {
            s = AuthModule.checkToken(cmdMethod, session, session.getRequest().headers().get(HttpHeaderNames.AUTHORIZATION));
        }
        return s;
    }

    @Override
    default String checkSign(Method cmdMethod, HttpSession session, ObjMap putData) {
        return checkAuth(cmdMethod, session, putData);
    }

}
