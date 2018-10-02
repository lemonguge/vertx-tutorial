package cn.homjie.vertx.tutorial.juc;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jiehong.jh
 * @date 2018/7/20
 */
public class SimpleThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(0);
    private String name;

    public SimpleThreadFactory(String threadName) {
        this.name = threadName;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        return new Thread(runnable, name + "-" + threadNumber.getAndIncrement());
    }
}
