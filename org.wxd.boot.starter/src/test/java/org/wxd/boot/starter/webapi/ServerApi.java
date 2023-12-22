package org.wxd.boot.starter.webapi;

import com.google.inject.Inject;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.net.controller.ann.TextController;
import org.wxd.boot.net.controller.ann.TextMapping;
import org.wxd.boot.net.web.hs.HttpSession;
import org.wxd.boot.starter.IocContext;
import org.wxd.boot.starter.i.IBeanInit;
import org.wxd.boot.starter.service.HsService;

/**
 * 公共
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-13 18:50
 **/
@TextController()
public class ServerApi implements IBeanInit {

    @Inject HsService hsService;

    @Override public void beanInit(IocContext iocContext) throws Exception {

    }

    @TextMapping(remarks = "sdk处理")
    public String index(HttpSession httpSession, ObjMap objMap) {
        return "holle";
    }

    @TextMapping(remarks = "sdk处理", needAuth = 1)
    public String sdk(HttpSession httpSession, ObjMap objMap) {
        return "sdk";
    }

}
