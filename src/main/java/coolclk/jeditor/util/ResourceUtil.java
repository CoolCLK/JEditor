package coolclk.jeditor.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ResourceUtil {
    /**
     * 列出 JAR 资源文件夹下所有文件/文件夹
     * @author CoolCLK
     */
    public static List<URL> listResources(String name) {
        List<URL> list = new ArrayList<>();
        try {
            Enumeration<URL> resources = ResourceUtil.class.getClassLoader().getResources(name);
            while (resources.hasMoreElements()) {
                list.add(resources.nextElement());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
