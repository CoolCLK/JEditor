package coolclk.jeditor.util;

/**
 * 数学类工具
 * @author CoolCLK
 */
public class MathUtil {
    /**
     * 检查一个值是否在范围内
     * @param ranged 被检查的值
     * @param a 范围最值
     * @param b 范围最值
     * @return 值如果在 a, b 两参中则返回 {@link java.lang.Boolean#TRUE} , 否则返回 {@link java.lang.Boolean#FALSE}
     * @author CoolCLK
     */
    public static boolean range(int ranged, int a, int b) {
        return ranged >= Math.min(a, b) && ranged <= Math.max(a, b);
    }
}
