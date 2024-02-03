package coolclk.jeditor.api.lang;

import java.util.ArrayList;
import java.util.List;

public class AutoStoppableThread extends StoppableThread {
    private final static List<AutoStoppableThread> THREAD_POOL = new ArrayList<>();
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (AutoStoppableThread thread : THREAD_POOL) {
                thread.stop();
            }
        }, "AutoCloseableThread"));
    }

    public AutoStoppableThread() {
        this(null, (Runnable) null);
    }

    public AutoStoppableThread(Runnable target) {
        this(null, target);
    }

    public AutoStoppableThread(ThreadGroup group, Runnable target) {
        this(group, target, "AutoCloseableThread-" + nextThreadNum());
    }

    public AutoStoppableThread(String name) {
        this(null, null, name);
    }

    public AutoStoppableThread(ThreadGroup group, String name) {
        this(group, null, name);
    }

    public AutoStoppableThread(Runnable target, String name) {
        this(null, target, name);
    }

    public AutoStoppableThread(ThreadGroup group, Runnable target, String name) {
        this(group, target, name, 0);
    }

    public AutoStoppableThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, target, name, stackSize);
        THREAD_POOL.add(this);
    }

    private static int threadInitNumber;
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }
}
