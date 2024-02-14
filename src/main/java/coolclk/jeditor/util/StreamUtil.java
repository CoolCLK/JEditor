package coolclk.jeditor.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StreamUtil {
    /**
     * 从头读取输入流所有数据
     * @throws IOException 读取输入流时错误
     * @author CoolCLK
     */
    public static byte[] readAllBytes(InputStream stream) throws IOException {
        stream.reset();
        return readToEnd(stream);
    }

    /**
     * 读取输入流一直到结尾的所有数据
     * @throws IOException 读取输入流时错误
     * @author CoolCLK
     */
    public static byte[] readToEnd(InputStream stream) throws IOException {
        List<Byte> bytes = new ArrayList<>();
        int b;
        while ((b = stream.read()) != -1) {
            bytes.add((byte) b);
        }
        return byteArrayToBytes(bytes.toArray(new Byte[0]));
    }

    /**
     * 将基元数据 byte[] 转换为 {@link java.lang.Byte}[]
     * @author CoolCLK
     */
    public static Byte[] bytesToByteArray(byte[] bytes) {
        Byte[] byteArray = new Byte[bytes.length];
        for (int i = 0; i < byteArray.length; i++) {
            byteArray[i] = bytes[i];
        }
        return byteArray;
    }

    /**
     * 将{@link java.lang.Byte}[] 转换为 基元数据 byte[]
     * @author CoolCLK
     */
    public static byte[] byteArrayToBytes(Byte[] byteArray) {
        byte[] bytes = new byte[byteArray.length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = byteArray[i];
        }
        return bytes;
    }
}
