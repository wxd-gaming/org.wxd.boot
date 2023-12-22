package org.wxd.boot.net.auth;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-02-09 17:23
 **/
public interface IAuth {

    default String getUserName() {
        return null;
    }

    default String getToken() {
        return null;
    }

    default IAuth setToken(String token) {
        return null;
    }

    default boolean checkAuth(int auth) {
        return true;
    }

}
