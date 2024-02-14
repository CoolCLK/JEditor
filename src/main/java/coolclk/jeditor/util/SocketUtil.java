package coolclk.jeditor.util;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketUtil {
    /**
     * 检测地址上的一个端口是否被使用
     * @author CoolCLK
     */
    public static boolean isPortUsed(int port, InetAddress address) {
        try (Socket s = new Socket(address, port)) {
            return s.isConnected();
        }
        catch (Exception ignored) {  }
        return false;
    }

    /**
     * 检测<strong>本地地址</strong>上的一个端口是否被使用
     * @author CoolCLK
     */
    public static boolean isPortUsed(int port) {
        try {
            return isPortUsed(port, InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
