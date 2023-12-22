package org.wxd.boot.net.auth;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.net.Session;

import java.lang.reflect.Method;

/**
 * 登录验证
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-05-21 19:27
 **/
public interface SignCheck<S extends Session> {

    static String checkAuth(Method cmdMethod, Session session, ObjMap putData) {
        return AuthModule.checkToken(cmdMethod, session, putData.getString(HttpHeaderNames.AUTHORIZATION.toString()));
    }

    default String checkSign(Method cmdMethod, S session, ObjMap putData) {
        return checkAuth(cmdMethod, session, putData);
    }

}
