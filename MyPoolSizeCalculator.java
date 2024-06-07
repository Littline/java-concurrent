import java.math.BigDecimal;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

// [最优线程数 = CPU核心数 \times 目标利用率 \times \left(1 + \frac{等待时间}{计算时间}\right)]
public class MyPoolSizeCalculator extends PoolSizeCalculator {

    public static void main(String[] args) {
        // 初始化PoolSizeCalculator，可以根据实际情况调整参数
        MyPoolSizeCalculator calculator = new MyPoolSizeCalculator(100, 100, 10000);

        // 设置目标利用率和目标队列大小
        BigDecimal targetUtilization = new BigDecimal("0.75"); // 目标CPU利用率
        BigDecimal targetQueueSizeBytes = new BigDecimal(1024 * 1024); // 目标队列大小，假设为1MB

        // 计算最优线程池大小和队列容量
        calculator.calculateBoundaries(targetUtilization, targetQueueSizeBytes);
    }
    public MyPoolSizeCalculator(int sampleQueueSize, int accuracyMargin, long testDuration) {
        super(sampleQueueSize, accuracyMargin, testDuration);
    }

    @Override
    protected Runnable createTask() {
        return new Runnable() {
            @Override
            public void run() {
                // 模拟一些计算工作，这里可以根据您的实际需求进行调整
                double value = Math.random() * Math.random();
            }
        };
    }

    @Override
    protected BlockingQueue<Runnable> createWorkQueue() {
        // 根据需要选择合适的队列大小
        return new ArrayBlockingQueue<>(100);
    }

    @Override
    protected long getCurrentThreadCPUTime() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return threadMXBean.isCurrentThreadCpuTimeSupported() ?
                threadMXBean.getCurrentThreadCpuTime() : 0L;
    }
}
