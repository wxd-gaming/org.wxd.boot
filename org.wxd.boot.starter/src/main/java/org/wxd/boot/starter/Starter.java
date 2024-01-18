package org.wxd.boot.starter;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wxd.boot.AAAAA;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.function.Consumer2;
import org.wxd.boot.agent.function.ConsumerE2;
import org.wxd.boot.agent.function.STVFunction1;
import org.wxd.boot.agent.io.FileReadUtil;
import org.wxd.boot.agent.system.ReflectContext;
import org.wxd.boot.collection.OfSet;
import org.wxd.boot.starter.action.ActionProtoController;
import org.wxd.boot.starter.action.ActionTextController;
import org.wxd.boot.starter.action.ActionTimer;
import org.wxd.boot.starter.i.*;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.str.json.ProtobufMessageSerializerFastJson;
import org.wxd.boot.system.BytesUnit;
import org.wxd.boot.system.DumpUtil;
import org.wxd.boot.system.GlobalUtil;
import org.wxd.boot.system.JvmUtil;
import org.wxd.boot.threading.Executors;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 启动器
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-11 19:11
 **/
public class Starter {

    private static volatile IocContext mainIocInjector = null;
    private static volatile IocContext childIocInjector = null;

    public static IocContext curIocInjector() {
        if (childIocInjector == null) return mainIocInjector;
        return childIocInjector;
    }

    public static void startBoot(Class... startClasses) {
        final String[] packages = Arrays.stream(startClasses)
                .map(v -> v.getPackage().getName())
                .toArray(String[]::new);
        startBoot(packages);
    }

    public static void startBoot(String... packages) {
        JvmUtil.setLogbackConfig();
        Set<String> packages1 = OfSet.asSet(packages);
        packages1.add(AAAAA.class.getPackage().getName());
        String[] array = packages1.toArray(new String[0]);
        if (mainIocInjector != null) throw new RuntimeException("不允许第二次启动");
        ReflectContext.Builder builder = ReflectContext.Builder.of(array);
        try {
            ReflectContext reflectContext = builder.build();

            List<BaseModule> list = Stream.concat(
                            Stream.of(new BootStarterModule(reflectContext), new StarterModule(reflectContext)),
                            reflectContext.classWithSuper(UserModule.class).map(v -> {
                                try {
                                    return v
                                            .getDeclaredConstructor(ReflectContext.class)
                                            .newInstance(reflectContext);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            })
                    )
                    .map(v -> (BaseModule) v)
                    .toList();

            Injector injector = Guice.createInjector(Stage.PRODUCTION, list);
            mainIocInjector = injector.getInstance(IocMainContext.class);
            iocInitBean(mainIocInjector, reflectContext);
            JvmUtil.addShutdownHook(() -> {
                logger().info("------------------------------停服信号处理------------------------------");
                {
                    STVFunction1<Object, IShutdownBefore> shutdownBefore = IShutdownBefore::shutdownBefore;
                    curIocInjector().beanStream(IShutdownBefore.class, shutdownBefore).forEach(object -> {
                        try {
                            logger().info("shutdownBefore：{} {}", object.getClass(), object.toString());
                            object.shutdownBefore();
                        } catch (Throwable e) {
                            throw Throw.as(object.getClass().getName() + ".shutdown()", e);
                        }
                    });
                }
                {
                    STVFunction1<Object, IShutdown> shutdown = IShutdown::shutdown;
                    curIocInjector().beanStream(IShutdown.class, shutdown).forEach(object -> {
                        try {
                            logger().info("shutdown：{} {}", object.getClass(), object.toString());
                            object.shutdown();
                        } catch (Throwable e) {
                            throw Throw.as(object.getClass().getName() + ".shutdown()", e);
                        }
                    });
                }
                {
                    STVFunction1<Object, IShutdownEnd> shutdown = IShutdownEnd::shutdownEnd;
                    curIocInjector().beanStream(IShutdownEnd.class, shutdown).forEach(object -> {
                        try {
                            logger().info("shutdownEnd：{} {}", object.getClass(), object.toString());
                            object.shutdownEnd();
                        } catch (Throwable e) {
                            throw Throw.as(object.getClass().getName() + ".shutdown()", e);
                        }
                    });
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                logger().info("------------------------------停服处理结束------------------------------");
                JvmUtil.halt(0);
            });
            logger().info("主容器初始化完成：{}", mainIocInjector.hashCode());

            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                /*全局未捕获线程异常*/
                @Override public void uncaughtException(Thread t, Throwable e) {
                    try {
                        System.out.println(t);
                        e.printStackTrace(System.out);
                    } catch (Throwable t0) {}
                }
            });

            GlobalUtil.exceptionCall = new Consumer2<Object, Throwable>() {
                @Override public void accept(Object o, Throwable throwable) {
                    FeishuPack.Default.asyncFeiShuNotice("异常", String.valueOf(o), throwable);
                }
            };

        } catch (Throwable throwable) {
            logger().error("启动失败", throwable);
            JvmUtil.halt(-1);
        }
    }

    public static IocContext createChildInjector(ReflectContext reflectContext) {
        return childIocInjector = createChildInjector(mainIocInjector, reflectContext);
    }

    public static IocContext createChildInjector(IocContext parentContext, ReflectContext reflectContext) {
        try {

            List<BaseModule> modules = Stream.concat(
                            Stream.of(new StarterModule(reflectContext, IocSubContext.class)),
                            reflectContext.classWithSuper(UserModule.class).map(v -> {
                                try {
                                    return v
                                            .getDeclaredConstructor(ReflectContext.class)
                                            .newInstance(reflectContext);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            })
                    )
                    .map(v -> (BaseModule) v)
                    .toList();

            Injector injector = parentContext.getInjector().createChildInjector(modules);
            IocContext iocInjector = injector.getInstance(IocSubContext.class);
            iocInitBean(iocInjector, reflectContext);
            logger().info("子容器初始化完成：{}", iocInjector.hashCode());
            return iocInjector;
        } catch (Throwable throwable) {
            throw Throw.as("子容器初始化失败", throwable);
        }
    }

    static void iocInitBean(IocContext context, ReflectContext reflectContext) throws Exception {
        if (logger().isDebugEnabled()) {
            long count = reflectContext.getContentList().size();
            logger().debug("find class size ：" + count);
        }

        /*todo fastjson 注册 protoBuff 处理 处理配置类*/
        reflectContext.classStream()
                .forEach(ProtobufMessageSerializerFastJson::action);

        /*处理定时器资源*/
        ActionTimer.action(context, reflectContext);
        /*处理 ProtoController 资源*/
        ActionProtoController.action(context, reflectContext);
        /*http 处理器加载*/
        ActionTextController.action(context, reflectContext);

        /*todo 调用 init 方法 father */
        ConsumerE2<IBeanInit, IocContext> startFun = IBeanInit::beanInit;
        context.beanStream(IBeanInit.class, startFun).forEach(iBeanInit -> {
            if (reflectContext.classStream().anyMatch(v -> v.equals(iBeanInit.getClass()))) {
                try {
                    iBeanInit.beanInit(context);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                logger().debug("bean init {}", iBeanInit.getClass());
            }
        });
    }

    public static void start(boolean debug, int serverId, String serverName, String... extInfos) {
        {
            ConsumerE2<IStart, IocContext> startFun = IStart::start;
            curIocInjector().forEachBean(IStart.class, startFun, curIocInjector());
        }
        {
            ConsumerE2<IStartEnd, IocContext> startEndFun = IStartEnd::startEnd;
            curIocInjector().forEachBean(IStartEnd.class, startEndFun, curIocInjector());
        }
        Executors.getDefaultExecutor().scheduleAtFixedDelay(
                () -> {
                    StringBuilder stringBuilder = new StringBuilder();
                    long freeMemory = DumpUtil.freeMemory(stringBuilder);
                    if (freeMemory < BytesUnit.Mb.toBytes(300)) {
                        logger().info(stringBuilder.toString());
                    }
                },
                30, 30, TimeUnit.SECONDS
        );
        print(debug, serverId, serverName, extInfos);
    }

    public static void print(boolean debug, int serverId, String serverName, String... extInfos) {
        StringBuilder stringAppend = new StringBuilder(1024);

        String printString = FileReadUtil.readString("print.txt");

        int len = 60;

        stringAppend.append("\n\n")
                .append(printString)
                .append("\n")
                .append("    -[ " + StringUtil.padRight("debug = " + debug + " | " + JvmUtil.processIDString(), len, ' ') + " ]-\n")
                .append("    -[ " + StringUtil.padRight(serverId + " | " + serverName, len, ' ') + " ]-\n")
                .append("    -[ " + StringUtil.padRight(JvmUtil.timeZone(), len, ' ') + " ]-\n");
        for (String extInfo : extInfos) {
            stringAppend.append("    -[ " + StringUtil.padRight(extInfo, len, ' ') + " ]-\n");
        }
        stringAppend.append("\n");
        logger().warn(stringAppend.toString());
    }

    static Logger logger() {
        return LoggerFactory.getLogger(Starter.class);
    }

}
