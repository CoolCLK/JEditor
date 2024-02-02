package coolclk.jeditor.util;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketUtil {
    public static boolean isPortUsed(int port, InetAddress address) {
        try (Socket s = new Socket(address, port)) {
            return s.isConnected();
        }
        catch (Exception ignored) {  }
        return false;
    }

    public static boolean isPortUsed(int port) {
        try {
            return isPortUsed(port, InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
