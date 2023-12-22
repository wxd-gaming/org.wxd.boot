package org.wxd.boot.net.auth;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.wxd.boot.agent.system.Base64Util;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.lang.SyncJson;
import org.wxd.boot.net.Session;
import org.wxd.boot.net.controller.ann.TextMapping;
import org.wxd.boot.str.StringUtil;

import java.util.Objects;

/**
 * 登录验证
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-05-21 19:27
 **/
public interface Sign<S extends Session> {

    @TextMapping(url = "/", remarks = "登录")
    default SyncJson sign(S session, ObjMap putData) {
        String username = putData.getString("userName");
        String userPwd = putData.getString("userPwd");
        userPwd = Base64Util.decode(userPwd);
        return signAuth(username, userPwd);
    }

    /**
     * @param username
     * @param md5Pwd   md5加密的密码
     * @return
     * @throws Exception
     */
    default SyncJson signAuth(String username, String md5Pwd) {
        final SignConfig signConfig = SignConfig.get();
        if (signConfig != null) {
            final IAuth sign = signConfig.optByUser(username).orElse(null);
            if (sign == null) {
                return SyncJson.error(101, "账号不存在");
            }

            if (!Objects.equals(sign.getToken(), md5Pwd)) {
                return SyncJson.error(101, "密码错误");
            }
            String token = cacheToken(sign);
            return SyncJson.ok().append(HttpHeaderNames.AUTHORIZATION.toString(), token);
        }
        return SyncJson.error(100, "账号不存在");
    }

    default String cacheToken(IAuth auth) {

        String token = StringUtil.getRandomString(4) +
                "." + StringUtil.getRandomString(4) +
                "." + StringUtil.getRandomString(4) +
                "." + (System.currentTimeMillis() / 1000);

        AuthModule.AUTH_CACHE_PACK.addCache(token, auth);

        return token;
    }

}
