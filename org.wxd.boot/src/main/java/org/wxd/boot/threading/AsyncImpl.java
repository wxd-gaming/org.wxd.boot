package org.wxd.boot.threading;

import org.wxd.boot.agent.system.AnnUtil;
import org.wxd.boot.str.StringUtil;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 异步化处理
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-04 19:23
 **/
public class AsyncImpl {

    public static boolean asyncAction(AtomicBoolean vt,
                                      AtomicReference<String> threadName,
                                      AtomicReference<String> queueName,
                                      Method method) {
        Async ann = AnnUtil.ann(method, Async.class);
        if (ann != null) {
            vt.set(ann.vt());
            if (StringUtil.notEmptyOrNull(ann.threadName())) {
                threadName.set(ann.threadName());
            }
            if (StringUtil.notEmptyOrNull(ann.queueName())) {
                queueName.set(ann.queueName());
            }
            return true;
        }
        return false;
    }

}
