package coolclk.jeditor.util;

import java.util.Objects;

public class StringUtil {
    private final String target;

    public StringUtil(String string) {
        this.target = string;
    }

    @Override
    public String toString() {
        return this.target;
    }

    /**
     * 统计目标值在被检查值出现次数
     * @author CoolCLK
     */
    public int appearTimes(Object targetObject) {
        return appearTimes(this, targetObject);
    }

    /**
     * 忽略正则表达式分割文本
     * @param regexObject 分割值, 不支持正则表达式, 若想要使用正则表达式请直接使用 {@link java.lang.String#split(String)}
     * @return 分割后结果
     * @author CoolCLK
     */
    public String[] split(Object regexObject) {
        return split(regexObject, 0);
    }

    /**
     * 忽略正则表达式分割文本
     * @param regexObject 分割值, 不支持正则表达式, 若想要使用正则表达式请直接使用 {@link java.lang.String#split(String, int)}
     * @param limit 最大分割次数, 不支持从结尾开始分割
     * @return 分割后结果
     * @author CoolCLK
     */
    public String[] split(Object regexObject, int limit) {
        return split(this, regexObject, limit);
    }

    /**
     * （可批量）忽略正则表达式替换文本
     * @param replacements 替换值, 以 1 为第一个参数的位置来说, 第奇数个参数为要替换值, 第偶数个参数为目标替换值, 若此参数的长度不为偶数将返回 null
     * @return 按输入参数顺序替换后结果
     * @author CoolCLK
     */
    public String replace(Object... replacements) {
        return replace(this, replacements);
    }

    /**
     * （可批量）忽略正则表达式替换文本
     * @param replacements 替换值, 以 1 为第一个参数的位置来说, 第奇数个参数为要替换值, 第偶数个参数为目标替换值, 若此参数的长度不为偶数将返回 null
     * @return 按输入参数顺序替换后结果
     * @author CoolCLK
     */
    public String replaceLast(Object... replacements) {
        return replaceLast(this, replacements);
    }

    /**
     * 统计目标值在被检查值出现次数
     * @author CoolCLK
     */
    public static int appearTimes(Object fromObject, Object targetObject) {
        String from = fromObject.toString(), target = targetObject.toString();
        int count = 0;
        int index = 0;
        while ((index = from.indexOf(target, index)) != -1) {
            index += target.length();
            count++;
        }
        return count;
    }

    /**
     * 忽略正则表达式分割文本
     * @param targetObject 被分割值
     * @param regexObject 分割值, 不支持正则表达式, 若想要使用正则表达式请直接使用 {@link java.lang.String#split(String)}
     * @return 分割后结果
     * @author CoolCLK
     */
    public static String[] split(Object targetObject, Object regexObject) {
        return split(targetObject, regexObject, 0);
    }

    /**
     * 忽略正则表达式分割文本
     * @param targetObject 被分割值
     * @param regexObject 分割值, 不支持正则表达式, 若想要使用正则表达式请直接使用 {@link java.lang.String#split(String, int)}
     * @param limit 最大分割次数, 不支持从结尾开始分割
     * @return 分割后结果
     * @author CoolCLK
     */
    public static String[] split(Object targetObject, Object regexObject, int limit) {
        boolean limited = limit > 0;
        String target = targetObject.toString(), regex =  regexObject.toString();
        char[] targetCharacters = target.toCharArray(), regexCharacters =  regex.toCharArray();

        String[] result = new String[(limited ? limit : appearTimes(target, regex) + 1)];
        int splitIndex = 0, resultIndex = 0;
        for (int i = 0; i < targetCharacters.length && (!limited || resultIndex < result.length); i++) {
            if (Objects.equals(targetCharacters[i], regexCharacters[0])) {
                for (int c = 0; c < regexCharacters.length; c++) {
                    if (!Objects.equals(targetCharacters[i + c], regexCharacters[c])) {
                        result[resultIndex] = target.substring(splitIndex, i);
                        splitIndex = i + regex.length();
                        resultIndex++;
                        break;
                    }
                }
            }
        }
        if (result[result.length - 1] == null) {
            result[result.length - 1] = target.substring(splitIndex);
        }
        return result;
    }

    /**
     * 忽略正则表达式分割文本并去掉最后一个项
     * @param targetObject 被分割值
     * @param regexObject 分割值, 不支持正则表达式, 若想要使用正则表达式请直接使用 {@link java.lang.String#split(String, int)}
     * @return 分割后结果
     * @author CoolCLK
     */
    public static String[] splitNoLast(Object targetObject, Object regexObject) {
        int limit = appearTimes(targetObject, regexObject);
        if (limit <= 0) {
            return new String[0];
        }
        return split(targetObject, regexObject, limit);
    }

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
                        i += (limit >= 0 ? 1 : -1) * (regexCharacters.length - 1);
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
