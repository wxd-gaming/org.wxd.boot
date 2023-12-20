package org.wxd.boot.net.handler;

import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.system.AnnUtil;
import org.wxd.boot.append.StreamWriter;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.lang.RunResult;
import org.wxd.boot.net.SocketSession;
import org.wxd.boot.net.controller.TextMappingRecord;
import org.wxd.boot.net.controller.cmd.ITokenCache;
import org.wxd.boot.net.controller.cmd.Sign;
import org.wxd.boot.net.controller.cmd.SignCheck;
import org.wxd.boot.str.json.FastJsonUtil;
import org.wxd.boot.system.GlobalUtil;
import org.wxd.boot.threading.EventRunnable;
import org.wxd.boot.threading.ExecutorLog;

import java.lang.reflect.Type;
import java.util.function.Consumer;

/**
 * 路由监听
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-19 15:38
 **/
@Slf4j
class CmdListenerAction extends EventRunnable {

    private final TextMappingRecord mappingRecord;
    private final ITokenCache tokenCache;
    private final SocketSession session;
    private final String listener;
    private final ObjMap putData;
    private final StreamWriter out;
    private final Consumer<Boolean> callBack;

    public CmdListenerAction(TextMappingRecord mappingRecord, ITokenCache tokenCache,
                             SocketSession session,
                             String listener, ObjMap putData,
                             StreamWriter out, Consumer<Boolean> callBack) {
        super(mappingRecord.method());
        this.mappingRecord = mappingRecord;
        this.tokenCache = tokenCache;
        this.session = session;
        this.listener = listener;
        this.putData = putData;
        this.out = out;
        this.callBack = callBack;
    }

    @Override public String getTaskInfoString() {
        return session.getIp() + listener;
    }

    @Override public void onEvent() {

        Sign sign;
        if (mappingRecord.instance() instanceof Sign) {
            sign = (Sign) mappingRecord.instance();
        } else {
            sign = null;
        }
        SignCheck signCheck;
        if (mappingRecord.instance() instanceof SignCheck) {
            signCheck = (SignCheck) mappingRecord.instance();
        } else {
            signCheck = null;
        }

        try {
            if (mappingRecord.path().endsWith("/sign")) {
                RunResult signResult = sign.sign(tokenCache, session, putData);
                out.write(signResult.toString());
            } else if (signCheck == null || signCheck.checkSign(out, tokenCache, mappingRecord.method(), session, putData)) {
                Object invoke;
                if (mappingRecord.method().getParameterCount() == 0) {
                    invoke = mappingRecord.method().invoke(mappingRecord.instance());
                } else {
                    Object[] params = new Object[mappingRecord.method().getParameterCount()];
                    if (mappingRecord.method().getParameterCount() > 0) {
                        Type[] genericParameterTypes = mappingRecord.method().getGenericParameterTypes();
                        for (int i = 0; i < params.length; i++) {
                            Type genericParameterType = genericParameterTypes[i];
                            if (genericParameterType instanceof Class<?>) {
                                if (genericParameterType.equals(StreamWriter.class)) {
                                    params[i] = out;
                                } else if (genericParameterType.equals(ObjMap.class)) {
                                    params[i] = putData;
                                } else if (((Class<?>) genericParameterType).isAssignableFrom(session.getClass())) {
                                    params[i] = session;
                                }
                            }
                        }
                    }
                    invoke = mappingRecord.method().invoke(mappingRecord.instance(), params);
                }
                Class<?> returnType = mappingRecord.method().getReturnType();
                if (!void.class.equals(returnType)) {
                    out.write(String.valueOf(invoke));
                }
            }
        } catch (Throwable throwable) {
            if (throwable.getCause() != null) {
                throwable = throwable.getCause();
            }
            String content = this.toString();
            content += "\n来源：" + session.toString();
            content += "\nAuth：" + session.getAuthUser();
            content += "\n执行：cmd = " + listener;
            content += "\n参数：" + FastJsonUtil.toJson(putData);
            log.error(content + " 异常", throwable);
            GlobalUtil.exception(content, throwable);
            out.clear();
            out.write(RunResult.error(505, Throw.ofString(throwable)));
        } finally {
            boolean showLog = false;
            ExecutorLog annotation = AnnUtil.ann(mappingRecord.method(), ExecutorLog.class);
            if (annotation != null) {
                showLog = annotation.showLog();
            }
            callBack.accept(showLog);
        }
    }

}