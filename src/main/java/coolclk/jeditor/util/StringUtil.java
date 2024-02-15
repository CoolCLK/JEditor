package coolclk.jeditor.util;

import java.util.Objects;

public class StringUtil {
    /**
     * （可批量）忽略正则表达式替换文本
     * @param target 被替换值
     * @param replacements 替换值, 以 1 为第一个参数的位置来说, 第奇数个参数为要替换值, 第偶数个参数为目标替换值, 若此参数的长度不为偶数将返回 null
     * @return 按输入参数顺序替换后结果
     * @author CoolCLK
     */
    public static String replace(Object target, Object... replacements) {
        return replace0(target, 1, replacements);
    }

    /**
     * （可批量）忽略正则表达式替换文本
     * @param target 被替换值
     * @param replacements 替换值, 以 1 为第一个参数的位置来说, 第奇数个参数为要替换值, 第偶数个参数为目标替换值, 若此参数的长度不为偶数将返回 null
     * @return 按输入参数顺序替换后结果
     * @author CoolCLK
     */
    public static String replaceLast(Object target, Object... replacements) {
        return replace0(target, -1, replacements);
    }

    /**
     * （可批量）忽略正则表达式替换文本
     * @param target 被替换值
     * @param replacements 替换值, 以 1 为第一个参数的位置来说, 第奇数个参数为要替换值, 第偶数个参数为目标替换值, 若此参数的长度不为偶数将返回 null
     * @return 按输入参数顺序替换后结果
     * @author CoolCLK
     */
    public static String replaceAll(Object target, Object... replacements) {
        return replace0(target, 0, replacements);
    }

    private static String replace0(Object target, int limit, Object... replacements) {
        if (Objects.isNull(target) || target.toString().isEmpty()) {
            return "";
        }
        if (replacements.length % 2 == 0) {
            String result = target.toString();
            for (int i = 0; i < replacements.length; i += 2) {
                result = replace1(result, replacements[i].toString(), replacements[i + 1].toString(), limit);
            }
            return result;
        }
        return null;
    }

    private static String replace1(String target, String regex, String replacement, int limit) {
        if (!target.isEmpty() && !regex.isEmpty()) {
            StringBuilder result = new StringBuilder();
            char[] targetCharacters = target.toCharArray();
            char[] regexCharacters = regex.toCharArray();
            int replaceCounter = 0;
            for (int i = (limit >= 0 ? 0 : targetCharacters.length - 1); (limit >= 0 ? i < targetCharacters.length : i >= 0); i += (limit >= 0 ? 1 : -1)) {
                if (Objects.equals(targetCharacters[i], regexCharacters[0])) {
                    boolean match = true;
                    for (int c = 0; i + c < targetCharacters.length && c < regexCharacters.length; c++) {
                        if (!Objects.equals(targetCharacters[i + c], regexCharacters[c])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        result.append(replacement);
                        i += (limit >= 0 ? 1 : -1) * regexCharacters.length;
                        replaceCounter++;
                        if (limit != 0 && replaceCounter >= Math.abs(limit)) {
                            break;
                        }
                    }
                } else {
                    result.append(targetCharacters[i]);
                }
            }
            return result.toString();
        }
        return "";
    }
}
