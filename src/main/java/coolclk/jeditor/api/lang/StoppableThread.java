package coolclk.jeditor.api.lang;

import java.lang.reflect.InvocationTargetException;

/**
 * 在 {@link java.lang.Thread} 基础上添加了可自定义关闭
 * @author CoolCLK
 */
public class StoppableThread {
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    private final Thread thread;
    private final Stoppable target;
    public StoppableThread() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        this(null, (Runnable) null);
    }

    public StoppableThread(Runnable target) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        this(null, target);
    }

    public StoppableThread(ThreadGroup group, Runnable target) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this(group, target, "Thread-" + Thread.class.getDeclaredMethod("nextThreadNum").invoke(null));
    }

    public StoppableThread(String name) {
        this(null, null, name);
    }

    public StoppableThread(ThreadGroup group, String name) {
        this(group, null, name);
    }

    public StoppableThread(Runnable target, String name) {
        this(null, target, name);
    }

    public StoppableThread(ThreadGroup group, Runnable target, String name) {
        this(group, target, name, 0);
    }

    public StoppableThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        this.thread = new Thread(group, target, name, stackSize);
        if (target instanceof Stoppable) {
            this.target = (Stoppable) target;
        } else {
            this.target = null;
        }
    }

    public synchronized void start() {
        thread.start();
    }

    public synchronized void stop() {
        target.stop();
    }
}
