package coolclk.jeditor.api.lang;

/**
 * 在 {@link java.lang.Runnable} 的基础上又添加了 {@link Stoppable#stop()}
 * @author CoolCLK
 */
public interface Stoppable extends Runnable {
    public abstract void stop();
}
