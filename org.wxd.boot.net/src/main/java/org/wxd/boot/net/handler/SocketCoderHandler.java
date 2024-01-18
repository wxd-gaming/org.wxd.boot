package org.wxd.boot.net.handler;

import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wxd.boot.agent.system.AnnUtil;
import org.wxd.boot.agent.zip.GzipUtil;
import org.wxd.boot.append.StreamWriter;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.lang.SyncJson;
import org.wxd.boot.net.SocketSession;
import org.wxd.boot.net.controller.MappingFactory;
import org.wxd.boot.net.controller.MessageController;
import org.wxd.boot.net.controller.ProtoMappingRecord;
import org.wxd.boot.net.controller.TextMappingRecord;
import org.wxd.boot.net.message.MessagePackage;
import org.wxd.boot.net.message.Rpc;
import org.wxd.boot.net.message.RpcEvent;
import org.wxd.boot.net.message.UpFileAccess;
import org.wxd.boot.net.util.ByteBufUtil;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.str.json.FastJsonUtil;
import org.wxd.boot.system.GlobalUtil;
import org.wxd.boot.system.MarkTimer;
import org.wxd.boot.threading.Async;
import org.wxd.boot.threading.ExecutorLog;
import org.wxd.boot.threading.Executors;
import org.wxd.boot.threading.IExecutorServices;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * socket 编解码器
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-08-05 14:22
 */
public interface SocketCoderHandler<S extends SocketSession> extends Serializable {

    Logger log = LoggerFactory.getLogger(SocketCoderHandler.class);

    String getName();

    /**
     * 如果没有找到监听，调用
     */
    INotController<S> getOnNotController();

    /** 设置没有监听调用的消息处理 */
    SocketCoderHandler<S> setOnNotController(INotController<S> onNotController);

    /** 执行前处理 */
    Predicate<MessageController> msgExecutorBefore();

    /** 执行前处理 */
    SocketCoderHandler<S> msgExecutorBefore(Predicate<MessageController> consumer);

    /** 处理网络接受到的消息字节 */
    default void read(S session, ByteBuf byteBuf) {
        session.checkReadTime();
        /*netty底层每一次传递的bytebuf都是最新的所以必须缓存*/
        ByteBuf tmpByteBuf;
        if (session.getReadByteBuf() == null) {
            tmpByteBuf = byteBuf;
        } else {
            tmpByteBuf = session.getReadByteBuf().writeBytes(byteBuf);
            ByteBufUtil.release(byteBuf);
        }

        // 读取 消息长度（int）和消息ID（int） 需要 8 个字节
        while (tmpByteBuf.readableBytes() >= 8) {
            // 读取消息长度
            tmpByteBuf.markReaderIndex();
            int len = tmpByteBuf.readInt();
            if (len > 0 && tmpByteBuf.readableBytes() >= len) {
                /*读取消息ID*/
                int messageId = tmpByteBuf.readInt();
                /*选择压缩*/
                // byte isZip = tmpByteBuf.readByte();
                byte[] messageBytes = new byte[len - 4];
                /*读取报文类容*/
                tmpByteBuf.readBytes(messageBytes);
                action(
                        session,
                        messageId,
                        messageBytes
                );
                session.addReadCount();
            } else {
//                if (log.isDebugEnabled()) {
//                    log.debug("剩余可读长度：", tmpByteBuf.readableBytes(), ", 不足：", len, ", ", this.toString());
//                }
                /*重新设置读取进度*/
                tmpByteBuf.resetReaderIndex();
                break;
            }
        }

        if (tmpByteBuf.readableBytes() > 0) {
            tmpByteBuf.discardReadBytes();
            session.setReadByteBuf(tmpByteBuf);
        } else {
            ByteBufUtil.release(tmpByteBuf);
            session.setReadByteBuf(null);
        }
    }

    default void action(S session, int messageId, byte[] messageBytes) {
        try {
            if (messageId == MessagePackage.getMessageId(Rpc.ReqRemote.class)) {
                Rpc.ReqRemote reqSyncMessage = Rpc.ReqRemote.parseFrom(messageBytes);
                log.debug("收到消息：{} {}{}", session, reqSyncMessage.getClass().getSimpleName(), FastJsonUtil.toJson(reqSyncMessage));
                long rpcId = reqSyncMessage.getRpcId();
                String params = reqSyncMessage.getParams();
                if (reqSyncMessage.getGzip() == 1) {
                    params = GzipUtil.unGzip2String(params);
                }
                /*处理消息--理论上是丢出去了的*/
                final ObjMap putData = FastJsonUtil.parse(params, ObjMap.class);
                String cmd = reqSyncMessage.getCmd().toLowerCase();
                switch (cmd) {
                    case "rpc.heart" -> {
                        session.rpcResponse(rpcId, "OK!");
                    }
                    case "rpc.upload.file.head" -> {
                        /*todo 接收文件头*/
                        long fileId = putData.getLongValue("fileId");
                        int bodyCount = putData.getIntValue("bodyCount");
                        String objectString = putData.getString("params");
                        UpFileAccess.readHead(fileId, bodyCount, objectString);
                        session.rpcResponse(rpcId, "OK!");
                    }
                    case "rpc.upload.file.body" -> {
                        /*todo 接收文件内容*/
                        long fileId = putData.getLongValue("fileId");
                        int bodyId = putData.getIntValue("bodyId");
                        int offset = putData.getIntValue("offset");
                        byte[] datas = putData.getObject("datas");
                        UpFileAccess fileAccess = UpFileAccess.readBody(fileId, bodyId, offset, datas);
                        if (fileAccess.getReadMaxCount() <= bodyId) {
                            /*表示读取完成*/
                            upFile(fileAccess);
                        }
                        session.rpcResponse(rpcId, "OK!");
                    }
                    default -> {
                        if (StringUtil.emptyOrNull(cmd)) {
                            log.info("{} 命令参数 cmd , 未找到", session.getChannelContext());
                            if (rpcId > 0) {
                                session.rpcResponse(rpcId, SyncJson.error("命令参数 cmd , 未找到").toJson());
                            }
                            return;
                        }

                        final String methodNameLowerCase = cmd.toLowerCase().trim();
                        TextMappingRecord mappingRecord = MappingFactory.textMappingRecord(getName(), methodNameLowerCase);
                        if (mappingRecord == null) {
                            log.info("{} not found url {}", session.getChannelContext(), cmd);
                            if (rpcId > 0) {
                                session.rpcResponse(rpcId, SyncJson.error("not found url " + cmd).toJson());
                            }
                            return;
                        }
                        final MarkTimer markTimer = MarkTimer.build();
                        final StreamWriter outAppend = new StreamWriter(1024);
                        CmdListenerAction listenerAction = new CmdListenerAction(mappingRecord, session, cmd, putData, outAppend, (showLog) -> {
                            if (showLog) {
                                log.info("\n执行：" + this.toString()
                                        + "\n" + markTimer.execTime2String() +
                                        "\nrpcId=" + rpcId +
                                        "\ncmd = " + cmd + ", " + FastJsonUtil.toJson(putData) +
                                        "\n结果 = " + outAppend.toString());
                            }
                            if (rpcId > 0) {
                                session.rpcResponse(rpcId, outAppend.toString());
                            }
                        });
                        listenerAction.submit();
                    }
                }
                return;
            }

            if (messageId == MessagePackage.getMessageId(Rpc.ResRemote.class)) {
                Rpc.ResRemote resSyncMessage = Rpc.ResRemote.parseFrom(messageBytes);
                if (log.isDebugEnabled())
                    log.debug("收到消息：{} {}{}", session, resSyncMessage.getClass().getSimpleName(), FastJsonUtil.toJson(resSyncMessage));
                if (resSyncMessage.getRpcId() > 0) {
                    String params = resSyncMessage.getParams();
                    if (resSyncMessage.getGzip() == 1) {
                        params = GzipUtil.unGzip2String(params);
                    }
                    RpcEvent syncrequest = RpcEvent.RPC_REQUEST_CACHE_PACK.cache(resSyncMessage.getRpcId());
                    if (syncrequest != null) {
                        syncrequest.response(resSyncMessage.getParams());
                    } else {
                        log.info(
                                "{} 同步消息回来后，找不到同步对象 {}, rpcId={}, params={}",
                                getName(),
                                this.toString(),
                                resSyncMessage.getRpcId(),
                                params,
                                new RuntimeException()
                        );
                    }
                }
                return;
            }

            /*处理消息--理论上是丢出去了的*/
            onMessage(session, messageId, messageBytes);
        } catch (Exception e) {
            log.warn("读取消息异常", e);
            GlobalUtil.exception("读取消息异常 " + toString(), e);
        }
    }


    /**
     * 处理消息，并且派发到对应线程
     *
     * @param session      通信对象
     * @param messageId    消息id
     * @param messageBytes 消息报文
     */
    default void onMessage(S session, int messageId, byte[] messageBytes) {
        ProtoMappingRecord mapping = MappingFactory.protoMappingRecord(getName(), messageId);
        if (mapping != null) {
            try {
                Message message = MessagePackage.parseMessage(messageId, messageBytes);
                onMessage(session, messageId, message);
            } catch (Throwable e) {
                log.error(mapping.toString() + " -> 遇到错误", e);
            }
        } else {
            this.notController(session, messageId, messageBytes);
        }
    }

    /**
     * @param session   session
     * @param messageId 消息协议id
     * @param message   消息
     */
    default void onMessage(S session, int messageId, Message message) {
        executor(session, messageId, message);
    }

    /**
     * 二级代理可以设置附加参数
     *
     * @param session   session
     * @param messageId 消息协议id
     * @param message   消息
     */
    default void executor(S session, int messageId, Message message) {
        ProtoMappingRecord mapping = MappingFactory.protoMappingRecord(getName(), messageId);
        MessageController controller = new MessageController(mapping, session, message);

        if (msgExecutorBefore() != null) {
            if (!msgExecutorBefore().test(controller)) {
                if (log.isDebugEnabled()) {
                    log.debug("{} 请求：" + "\n{}", this.getClass().getSimpleName(), controller.toString(), new RuntimeException("被过滤掉"));
                }
                return;
            }
        }

        ExecutorLog protoMapping = AnnUtil.ann(mapping.method(), ExecutorLog.class);
        if (log.isDebugEnabled() || (protoMapping != null && protoMapping.showLog())) {
            log.info("{} 收到消息：" + "\n{}", this.getClass().getSimpleName(), controller.toString());
        }
        Async async = AnnUtil.ann(mapping.method(), Async.class);
        String queueName = "";
        IExecutorServices executorServices = Executors.getLogicExecutor();
        if (async != null) {
            if (StringUtil.notEmptyOrNull(async.threadName())) {
                executorServices = Executors.All_THREAD_LOCAL.get(async.threadName());
            } else if (async.vt()) {
                executorServices = Executors.getLogicExecutor();
            }
            queueName = async.queueName();
        }
        executorServices.submit(queueName, controller);
    }

    /**
     * 无法处理的消息
     */
    default void notController(S session, int messageId, byte[] messageBytes) {
        boolean showLog = true;
        if (this.getOnNotController() != null) {
            showLog = !this.getOnNotController().notController(session, messageId, messageBytes);
        }
        if (showLog) {
            log.warn("\n{},\n消息：{} (len = {}}) 没有找到 controller, \n来自：{}",
                    this.getClass().getSimpleName(),
                    MessagePackage.msgInfo(messageId),
                    messageBytes.length,
                    session.toString(),
                    new RuntimeException()
            );
        }
    }

    default void upFile(UpFileAccess upFileAccess) throws Exception {
        upFileAccess.saveFile();
    }

}
