import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

public abstract class PoolSizeCalculator {

    // 可配置的参数，以提高灵活性
    private final int sampleQueueSize;
    private final int accuracyMargin;
    private final long testDuration;

    private volatile boolean expired;

    public PoolSizeCalculator(int sampleQueueSize, int accuracyMargin, long testDuration) {
        this.sampleQueueSize = sampleQueueSize;
        this.accuracyMargin = accuracyMargin;
        this.testDuration = testDuration;
    }

    protected void calculateBoundaries(BigDecimal targetUtilization, BigDecimal targetQueueSizeBytes) {
        calculateOptimalCapacity(targetQueueSizeBytes);
        Runnable task = createTask();
        start(task); // warm up phase
        long cputime = getCurrentThreadCPUTime();
        start(task); // test interval
        cputime = getCurrentThreadCPUTime() - cputime;
        long waittime = (testDuration * 1000000) - cputime;
        calculateOptimalThreadCount(cputime, waittime, targetUtilization);
    }

    private void calculateOptimalCapacity(BigDecimal targetQueueSizeBytes) {
        long mem = calculateMemoryUsage();
        BigDecimal queueCapacity = targetQueueSizeBytes.divide(new BigDecimal(mem), RoundingMode.HALF_UP);
        System.out.println("Target queue memory usage (bytes): " + targetQueueSizeBytes);
        System.out.println("createTask() produced " + createTask().getClass().getName() + " which took " + mem + " bytes in a queue");
        System.out.println("Formula: " + targetQueueSizeBytes + " / " + mem);
        System.out.println("* Recommended queue capacity (bytes): " + queueCapacity);
    }

    private void calculateOptimalThreadCount(long cpu, long wait, BigDecimal targetUtilization) {
        BigDecimal waitTime = new BigDecimal(wait);
        BigDecimal computeTime = new BigDecimal(cpu);
        BigDecimal numberOfCPU = new BigDecimal(Runtime.getRuntime().availableProcessors());
        BigDecimal optimalThreadCount = numberOfCPU.multiply(targetUtilization)
                .multiply(new BigDecimal(1).add(waitTime.divide(computeTime, RoundingMode.HALF_UP)));
        System.out.println("Number of CPU: " + numberOfCPU);
        System.out.println("Target utilization: " + targetUtilization);
        System.out.println("Elapsed time (nanos): " + (testDuration * 1000000));
        System.out.println("Compute time (nanos): " + cpu);
        System.out.println("Wait time (nanos): " + wait);
        System.out.println("Formula: " + numberOfCPU + " * " + targetUtilization + " * (1 + " + waitTime + " / " + computeTime + ")");
        System.out.println("* Optimal thread count: " + optimalThreadCount);
    }

    public void start(Runnable task) {
        long start;
        int runs = 0;
        do {
            if (++runs > 5) {
                throw new IllegalStateException("Test not accurate");
            }

            expired = false;
            start = System.currentTimeMillis();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    expired = true;
                }
            }, testDuration);

            while (!expired) {
                task.run();
            }

            start = System.currentTimeMillis() - start;
            timer.cancel();
        } while (Math.abs(start - testDuration) > accuracyMargin);
        collectGarbage(3);
    }

    private void collectGarbage(int times) {
        for (int i = 0; i < times; i++) {
            System.gc();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public long calculateMemoryUsage() {
        BlockingQueue<Runnable> queue = createWorkQueue();
        for (int i = 0; i < sampleQueueSize; i++) {
            queue.add(createTask());
        }
        long mem0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        collectGarbage(15);
        long mem1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return (mem1 - mem0) / sampleQueueSize;
    }

    protected abstract Runnable createTask();
    protected abstract BlockingQueue<Runnable> createWorkQueue();
    protected abstract long getCurrentThreadCPUTime();
}
