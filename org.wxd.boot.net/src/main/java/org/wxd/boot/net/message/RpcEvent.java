package org.wxd.boot.net.message;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.zip.GzipUtil;
import org.wxd.boot.cache.CachePack;
import org.wxd.boot.lang.SyncJson;
import org.wxd.boot.net.SocketSession;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.MarkTimer;
import org.wxd.boot.threading.Executors;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 同步等待结果
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-01-28 20:51
 **/
@Slf4j
@Getter
@Accessors(chain = true)
public class RpcEvent {

    public static final CachePack<Long, RpcEvent> RPC_REQUEST_CACHE_PACK = new CachePack<Long, RpcEvent>()
            .setUnload((requestSync, s) -> {})
            .setCacheName("rpc-sync")
            .setCacheSurvivalTime(30000)
            .setCacheIntervalTime(1000);

    public static String RPC_TOKEN = "5cb703a024e54648a70da85890ed7dbb";
    public static long WaiteMill = 3000;
    public static AtomicLong idFormat = new AtomicLong();

    protected final BlockingQueue<Boolean> queue = new LinkedBlockingQueue<>(1);
    protected MarkTimer markTime;
    protected final SocketSession session;
    protected long rpcId;
    protected final String cmd;
    protected final String reqJson;
    protected boolean sendEnd;
    protected boolean res = false;
    protected String resJson = null;
    protected StackTraceElement[] threadStackTrace = null;

    public RpcEvent(SocketSession session, String cmd, String reqJson) {
        this.session = session;
        this.cmd = cmd;
        this.reqJson = reqJson;

    }

    /** 发送出去，不管了 */
    public void send() {
        if (sendEnd) return;
        Rpc.ReqRemote.Builder builder = Rpc.ReqRemote.newBuilder();
        builder.setRpcId(rpcId);
        builder.setCmd(cmd);
        if (reqJson.length() > 1024) {
            builder.setGzip(1);
            builder.setParams(GzipUtil.gzip2String(reqJson));
        } else {
            builder.setParams(reqJson);
        }
        session.writeFlush(builder.build());
        sendEnd = true;
    }

    /** 构建异步处理 */
    public CompletableFuture<RpcEvent> completableFuture() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        threadStackTrace = new StackTraceElement[stackTrace.length - 2];
        System.arraycopy(stackTrace, 2, threadStackTrace, 0, threadStackTrace.length);
        return CompletableFuture.supplyAsync(() -> {
            this.get();
            return RpcEvent.this;
        });
    }

    /** 成功才会回调，有异常不会回调 */
    public void asyncOkBySyncJson(Consumer<SyncJson> ok) {
        async(rpcEvent -> ok.accept(SyncJson.parse(rpcEvent.resJson)), null, null);
    }

    /** 成功才会回调，有异常不会回调 */
    public void asyncOkByString(Consumer<String> ok) {
        async(rpcEvent -> ok.accept(rpcEvent.resJson), null, null);
    }

    /** 成功才会回调，有异常不会回调 */
    public void async(Consumer<RpcEvent> ok) {
        async(ok, null, null);
    }

    /**
     * 异步完成
     *
     * @param complete 无论是否异常都会回调
     */
    public void asyncComplete(Consumer<RpcEvent> complete) {
        async(null, complete, null);
    }

    /**
     * 异步完成
     *
     * @param ok       成功才会回调，有异常不会回调
     * @param complete 无论是否异常都会回调
     */
    public void async(Consumer<RpcEvent> ok, Consumer<RpcEvent> complete) {
        async(ok, complete, null);
    }

    /**
     * 异步完成
     *
     * @param ok       成功才会回调，有异常不会回调
     * @param complete 异步完成后无论是否异常都会回调
     * @param error    出现异常调用
     */
    public CompletableFuture<RpcEvent> async(Consumer<RpcEvent> ok, Consumer<RpcEvent> complete, Consumer<Throwable> error) {
        return completableFuture()
                .thenApply(rpcEvent -> {
                    if (ok != null) {
                        try {
                            ok.accept(rpcEvent);
                        } catch (Throwable t) {
                            if (error == null) {
                                log.error("rpc 处理异常：" + cmd + ", " + reqJson, t);
                            } else {
                                error.accept(t);
                            }
                        }
                    }
                    return rpcEvent;
                })
                .whenComplete((rpcEvent, throwable) -> {
                    if (complete != null) {
                        try {
                            complete.accept(rpcEvent);
                        } catch (Throwable t) {
                            if (error == null) {
                                log.error("rpc 处理异常：" + cmd + ", " + reqJson, t);
                            } else {
                                error.accept(t);
                            }
                        }
                    }
                })
                .exceptionally(throwable -> {

                    if (throwable instanceof CompletionException) {
                        throwable = throwable.getCause();
                    }

                    if (error == null) {
                        log.error("rpc 处理异常：" + cmd + ", " + reqJson, throwable);
                    } else {
                        RuntimeException aThrow = new Throw(throwable.toString());
                        aThrow.setStackTrace(threadStackTrace);
                        error.accept(aThrow);
                    }

                    return RpcEvent.this;
                });
    }

    /** 同步请求，等待结果 */
    public String get() {
        return get(WaiteMill);
    }

    /** 同步请求，等待结果 */
    public String get(long timeoutMillis) {
        if (!res) {
            this.rpcId = idFormat.incrementAndGet();
            RPC_REQUEST_CACHE_PACK.addCache(this.getRpcId(), this);
            send();
            Boolean poll = null;
            try {
                if (timeoutMillis > 0) {
                    poll = this.queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
                } else poll = this.queue.take();
            } catch (InterruptedException e) {
                throw Throw.as(e);
            }
            if (StringUtil.emptyOrNull(this.resJson)) {
                throw new RuntimeException("get time out");
            }
        }
        return resJson;
    }

    /** 回调回来 */
    public void response(String resJson) {
        this.res = true;
        this.resJson = resJson;
        this.queue.add(true);
    }

    @Override public String toString() {
        return this.getClass().getSimpleName() + "{"
                + "rpcId=" + rpcId + ", "
                + "cmd=" + cmd + ", "
                + "reqJson=" + reqJson
                + '}';
    }
}
