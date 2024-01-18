package system;

import org.junit.Test;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * 内存测试
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-05 20:39
 **/
public class MemoryTest {

    @Test
    public void show() {
        gatherHeap();
    }


    /** 堆内存手机 */
    public void gatherHeap() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();

        //初始堆内存
        BigDecimal initSize = toMb(heapMemory.getInit());
        BigDecimal usedSize = toMb(heapMemory.getUsed());
        BigDecimal maxSize = toMb(heapMemory.getMax());
        //os已分配大小
        BigDecimal committedSize = toMb(heapMemory.getCommitted());

        String info = String.format("init:%sMB,max:%sMB,committed:%sMB,used:%sMB",
                initSize.floatValue(), maxSize.floatValue(), committedSize.floatValue(), usedSize.floatValue());

        System.out.println(info);
    }

    /** 字节转mb */
    private BigDecimal toMb(long bytes) {
        final long mInBytes = 1024 * 1024;
        //四舍五入保留一位小数
        return BigDecimal.valueOf(bytes).divide(BigDecimal.valueOf(mInBytes), 1, RoundingMode.HALF_UP);
    }

    /** GC信息收集 */
    public void gatherGc() {
        List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcMXBean : gcMXBeans) {
            //gc总计次数
            long count = gcMXBean.getCollectionCount();
            //gc名
            String name = gcMXBean.getName();
            //gc大致耗时
            long costInMill = gcMXBean.getCollectionTime();
            String memNames = Arrays.deepToString(gcMXBean.getMemoryPoolNames());

            String info = String.format("name:%s,count:%s,cost %dms,pool name:%s",
                    name, count, costInMill, memNames);
            System.out.println(info);
        }
    }
}
