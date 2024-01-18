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
import org.wxd.boot.threading.Event;
import org.wxd.boot.threading.Executors;
import org.wxd.boot.threading.OptFuture;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
    protected StackTraceElement[] stackTraceElements = null;

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

    public OptFuture<RpcEvent> async() {
        return sendAsync(3);
    }

    public void async(Consumer<RpcEvent> consumer) {
        sendAsync(3)
                .subscribe(consumer)
                .onError(this::actionThrowable);
    }

    public OptFuture<String> asyncString() {
        return sendAsync(3).map(RpcEvent::getResJson);
    }

    public void asyncString(Consumer<String> consumer) {
        sendAsync(3)
                .subscribe(rpcEvent -> consumer.accept(rpcEvent.getResJson()))
                .onError(this::actionThrowable);
    }

    public OptFuture<SyncJson> asyncSyncJson() {
        return sendAsync(3).map(rpcEvent -> SyncJson.parse(rpcEvent.getResJson()));
    }

    public void asyncSyncJson(Consumer<SyncJson> consumer) {
        sendAsync(3)
                .subscribe(rpcEvent -> consumer.accept(SyncJson.parse(rpcEvent.getResJson())))
                .onError(this::actionThrowable);
    }

    OptFuture<RpcEvent> sendAsync(int stackTraceIndex) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        stackTraceElements = new StackTraceElement[stackTrace.length - stackTraceIndex];
        System.arraycopy(stackTrace, stackTraceIndex, stackTraceElements, 0, stackTraceElements.length);
        OptFuture<RpcEvent> optFuture = OptFuture.empty();
        Executors.getVTExecutor().submit(new Event(150, 1500) {
            @Override public String getTaskInfoString() {
                return RpcEvent.this.toString();
            }

            @Override public void onEvent() throws Exception {
                try {
                    get();
                    optFuture.complete(RpcEvent.this);
                } catch (Throwable throwable) {
                    RuntimeException runtimeException = Throw.as(RpcEvent.this.toString(), throwable);
                    if (stackTraceElements != null) {
                        runtimeException.setStackTrace(stackTraceElements);
                    }
                    optFuture.completeExceptionally(runtimeException);
                }
            }
        }, stackTraceIndex + 2);
        return optFuture;
    }

    public void actionThrowable(Throwable throwable) {
        log.error("{} url:{}", this.getClass().getSimpleName(), this.toString(), throwable);
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
