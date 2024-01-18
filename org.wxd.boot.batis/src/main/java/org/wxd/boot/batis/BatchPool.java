package org.wxd.boot.batis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.collection.ConvertCollection;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.GlobalUtil;
import org.wxd.boot.system.MarkTimer;
import org.wxd.boot.threading.Event;
import org.wxd.boot.threading.Executors;
import org.wxd.boot.threading.IExecutorServices;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 批量操作
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-05-20 16:32
 **/
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public abstract class BatchPool implements AutoCloseable {

    protected final DecimalFormat decimalFormat = new DecimalFormat("#0.0000");
    protected final ReentrantLock relock = new ReentrantLock();

    /*总运行耗时*/
    protected volatile long allOperTimes = 0;
    /*总操作数量*/
    protected volatile long allOperSize = 0;
    /*每一次获取数据的量*/
    protected volatile int batchSize = 2000;
    protected volatile int cacheSize = 0;
    protected volatile int maxCacheSize = 30000;
    protected volatile long showLogCd = 30 * 60 * 1000;
    protected volatile boolean runing = true;
    protected Batch_Work[] threads;

    public BatchPool(String threadName, int batchThreadSize) {
        if (batchThreadSize < 1) {
            batchThreadSize = 1;
        }
        if (batchThreadSize > 4) {
            batchThreadSize = 4;
        }
        threads = new Batch_Work[batchThreadSize];
        for (int i = 0; i < batchThreadSize; i++) {
            threads[i] = new Batch_Work(threadName, i);
        }
    }

    @Override
    public void close() throws InterruptedException {
        this.setRuning(false);
        log.info("-----------------等待数据落地处理------------------------");
        for (Batch_Work thread : threads) {
            thread.close();
        }
        log.info("-----------------数据落地处理完成------------------------");
    }

    protected abstract DataWrapper dataBuilder();

    protected abstract DbConfig dbConfig();

    /**
     * 异步插入数据库，有队列等待
     *
     * @param obj
     */
    public void replace(Object obj) {
        final EntityTable entityTable = dataBuilder().asEntityTable(obj);
        String tableName = entityTable.tableName(obj);
        int index = 0;
        if (threads.length >= 1) {
            /*批量入库的时候根据主键hash数据*/
            final Object fieldValue = entityTable.getDataColumnKey().getFieldValue(obj);
            index = StringUtil.hashIndex(fieldValue, true, threads.length);
        }
        Map<EntityField, Object> map = dataBuilder().toDbMap(obj);
        DataBuilder dataBuilder = new DataBuilder(obj, entityTable, map);
        threads[index].replace(tableName, dataBuilder);
    }

    volatile long lastTime = System.currentTimeMillis();

    protected class Batch_Work extends Event implements AutoCloseable {

        @Getter
        protected IExecutorServices executorServices;

        @Getter
        protected volatile HashMap<String, ConvertCollection<DataBuilder>> taskQueue = new HashMap<>();

        public Batch_Work(String threadName, int i) {
            super(500, TimeUnit.SECONDS.toMillis(30));
            executorServices = Executors.newExecutorServices(threadName + "-" + (i + 1), 1);
            executorServices.scheduleAtFixedDelay(this, 200, 200, TimeUnit.MILLISECONDS);
        }

        @Override public String getTaskInfoString() {
            return executorServices.getName();
        }

        public void replace(String tableName, DataBuilder obj) {
            relock.lock();
            try {

                boolean add = taskQueue.computeIfAbsent(tableName, k -> new ConvertCollection<>(batchSize))
                        .add(obj);
                if (add) {
                    BatchPool.this.cacheSize++;
                }
            } finally {
                relock.unlock();
            }
            if (BatchPool.this.cacheSize > BatchPool.this.maxCacheSize) {
                log.error("当前待处理的数据缓存超过：" + BatchPool.this.cacheSize + ", tableName = " + tableName, new RuntimeException("数据缓存"));
            }
        }

        protected Map.Entry<String, ConvertCollection<DataBuilder>> copy() {
            relock.lock();
            try {
                Iterator<Map.Entry<String, ConvertCollection<DataBuilder>>> iterator = taskQueue.entrySet().iterator();
                Map.Entry<String, ConvertCollection<DataBuilder>> next = null;
                if (iterator.hasNext()) {
                    next = iterator.next();
                    iterator.remove();
                    BatchPool.this.cacheSize -= next.getValue().size();
                }
                return next;
            } finally {
                relock.unlock();
            }
        }

        @Override
        public void close() throws InterruptedException {
            executorServices.shutdown();
            Thread.sleep(3000);
            while (!taskQueue.isEmpty() || !executorServices.isQueueEmpty()) {
                log.info("数据库异步处理!!!");
                run();
            }
        }

        @Override public void onEvent() {
            final Thread currentThread = Thread.currentThread();
            Map.Entry<String, ConvertCollection<DataBuilder>> copy = copy();
            if (copy == null) {
                return;
            }
            int i = 0;
            final MarkTimer markTimer = MarkTimer.build();
            try {
                final ConvertCollection<DataBuilder> copyValue = copy.getValue();

                while (copyValue.hasNext()) {
                    List<DataBuilder> tmp = copyValue.next();
                    /*同一张表结构*/
                    i += exec(copy.getKey(), tmp);
                }

                float execTime = markTimer.execTime();
                if (i == 0) {
                    log.error("replace.size() = " + copyValue.size() + ", oper = " + i, new RuntimeException());
                    return;
                }

                float dqjunzhi = (execTime / i);
                float dqsecjunzhi = 1000 / dqjunzhi;

                relock.lock();
                try {
                    if (Long.MAX_VALUE - allOperTimes < execTime) {
                        /*=统计超过最大值做清理操作*/
                        allOperTimes = 0;
                        allOperSize = 0;
                    }

                    allOperTimes += execTime;
                    allOperSize += i;
                } finally {
                    relock.unlock();
                }

                float junzhi = (allOperTimes * 1f / allOperSize);
                float secjunzhi = 1000 / junzhi;
                if (!isRuning()
                        || cacheSize > maxCacheSize
                        || ((System.currentTimeMillis() - lastTime) > showLogCd)
                        || dbConfig().isShow_sql()) {
                    lastTime = System.currentTimeMillis();
                    log.warn(
                            String.format("\n\n--------------------------------------------------" + currentThread.getName() + "-" + dbConfig().getDbBase() + "-异步写入-----------------------------------------------------------------------" +
                                            "\n当前操作总量：%s 条, 当前耗时：%s ms, 当前平均分布：%s ms/条, 当前性能：%s 条/S, " +
                                            "\n累计操作总量：%s 条, 历史耗时：%s ms, 历史平均分布：%s ms/条, 历史性能：%s 条/S, " +
                                            "\n当前剩余：%s 条未处理" +
                                            "\n--------------------------------------------------" + currentThread.getName() + "-" + dbConfig().getDbBase() + "-异步写入-----------------------------------------------------------------------\n",
                                    i, execTime, decimalFormat.format(dqjunzhi), decimalFormat.format(dqsecjunzhi),
                                    allOperSize, allOperTimes, decimalFormat.format(junzhi), decimalFormat.format(secjunzhi), cacheSize));
                    if (cacheSize > 300000) {
                        GlobalUtil.exception(currentThread.getName() + ", " + dbConfig().toString() + ", 当前数据堆积：" + cacheSize, null);
                    }
                }
            } catch (Throwable throwable) {
                throw Throw.as(throwable);
            }
        }
    }


    public abstract int exec(String tableName, List<DataBuilder> values) throws Exception;

}
