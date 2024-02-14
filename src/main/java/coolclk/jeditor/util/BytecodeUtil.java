package coolclk.jeditor.util;

import java.nio.charset.StandardCharsets;

public class BytecodeUtil {
    /**
     * 将 Java 运行时字节码转换为<s>具有可读性的</s>代码
     * @author CoolCLK
     */
    public static String toCode(byte[] bytecode) {
        return new String(bytecode);
    }

    /**
     * 将代码转换为<s> Java 运行时字节码</s>
     * @author CoolCLK
     */
    public static byte[] fromCode(String code) {
        return code.getBytes(StandardCharsets.UTF_8);
    }
}
