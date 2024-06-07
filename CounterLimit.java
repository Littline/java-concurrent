
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

// https://www.cnblogs.com/crazymakercircle/p/15187184.html#autoid-h3-3-0-2
// 计数器限流
public class CounterLimit {

    private static final Logger log = Logger.getLogger(CounterLimit.class.getName());

    // 起始时间
    private static long startTime = System.currentTimeMillis();
    // 时间区间的时间间隔 ms
    private static long interval = 1000;
    // 每秒限制数量
    private static long maxCount = 2;
    // 累加器
    private static AtomicLong accumulator = new AtomicLong();

    // 计数判断, 是否超出限制
    private static long tryAcquire(long taskId, int turn) {
        long nowTime = System.currentTimeMillis();
        // 在时间区间之内
        if (nowTime < startTime + interval) {
            long count = accumulator.incrementAndGet();

            if (count <= maxCount) {
                return count;
            } else {
                return -count;
            }
        } else {
            // 在时间区间之外
            synchronized (CounterLimit.class) {
                log.info("新时间区到了,taskId " + taskId + ", turn " + turn + "..");
                // 再一次判断，防止重复初始化
                if (nowTime > startTime + interval) {
                    accumulator.set(0);
                    startTime = nowTime;
                }
            }
            return 0;
        }
    }

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(10);

        // 被限制的次数
        AtomicInteger limited = new AtomicInteger(0);
        // 线程数
        final int threads = 2;
        // 每条线程的执行轮数
        final int turns = 20;
        // 同步器
        CountDownLatch countDownLatch = new CountDownLatch(threads);
        long start = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    for (int j = 0; j < turns; j++) {
                        long taskId = Thread.currentThread().getId();
                        long index = tryAcquire(taskId, j);
                        if (index <= 0) {
                            // 被限制的次数累积
                            limited.getAndIncrement();
                        }
                        Thread.sleep(200);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 等待所有线程结束
                countDownLatch.countDown();
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        float time = (System.currentTimeMillis() - start) / 1000F;
        // 输出统计结果
        log.info("限制的次数为：" + limited.get() + ",通过的次数为：" + (threads * turns - limited.get()));
        log.info("限制的比例为：" + (float) limited.get() / (float) (threads * turns));
        log.info("运行的时长为：" + time);
    }
}
