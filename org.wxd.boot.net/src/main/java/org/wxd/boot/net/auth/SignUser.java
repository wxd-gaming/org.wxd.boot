package org.wxd.boot.net.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.HashSet;
import java.util.Set;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-02-09 17:23
 **/

@Getter
@Setter
@Accessors(chain = true)
public class SignUser implements IAuth {

    /** 用户 */
    @Attribute
    private String userName;
    /** 密码 */
    @Attribute
    private String token;
    /** 权限 */
    @ElementList(inline = true, entry = "auth")
    private Set<Integer> authority = new HashSet<>();

    @Override
    public boolean checkAuth(int auth) {
        return authority.contains(auth);
    }

    @Override
    public String toString() {
        return "{" + "user='" + userName + '\'' + '}';
    }
}
