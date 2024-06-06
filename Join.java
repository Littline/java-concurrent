import java.util.concurrent.TimeUnit;

public class Join {
    public static void main(String[] args) throws Exception {
        Thread previous = Thread.currentThread();
        for (int i = 0; i < 10; i++) {
            // 每个线程拥有前一个线程的引用，需要等待前一个线程终止，才能从等待中返回
            Thread thread = new Thread(new Domino(previous), String.valueOf(i));
            thread.start();
            previous = thread;
        }
        TimeUnit.SECONDS.sleep(5);
        System.out.println(Thread.currentThread().getName() + " terminate." + "end");
    }
    static class Domino implements Runnable {
        private final Thread thread;
        public Domino(Thread thread) {
            System.out.println(thread.getName() + " initialized. ");
            this.thread = thread;
        }
        public void run() {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
            System.out.println(Thread.currentThread().getName() + " terminate.");
        }
    }
}