package coolclk.jeditor.util;

public class VersionUtil {
    public static class UnexpectedVersionFormat extends Exception {

    }

    /**
     * 比较两个版本的先后（仅适合用于比较 Java 版本）
     * @return 1 => 前一个输入参数版本后于后一个<br>
     *         0 => 前一个输入参数版本等同于后一个<br>
     *         -1 => 前一个输入参数版本早于后一个<br>
     * @author CoolCLK
     */
    public static int javaVersionCompare(String a, String b) {
        String[] aVersions = a.split("[._]");
        String[] bVersions = b.split("[._]");
        for (int i = 0; i < Math.max(aVersions.length, bVersions.length); i++) {
            int aVersion = 0, bVersion = 0;
            try {
                if (i < aVersions.length) {
                    aVersion = Integer.parseInt(aVersions[i]);
                }
                if (i < aVersions.length) {
                    bVersion = Integer.parseInt(bVersions[i]);
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException(new UnexpectedVersionFormat());
            }
            if (aVersion > bVersion) {
                return 1;
            } else if (aVersion < bVersion) {
                return -1;
            }
        }
        return 0;
    }
}
