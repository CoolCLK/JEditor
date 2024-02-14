package coolclk.jeditor.util;

import coolclk.jeditor.Application;
import javafx.scene.control.Alert;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class DesktopUtil {
    /**
     * 使用桌面程序唤起一个网页
     * @author CoolCLK
     */
    public static void openUrl(String url) {
        if (Desktop.isDesktopSupported()) {
            URI uri = URI.create(url);
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(uri);
                } catch (IOException e) {
                    new Alert(Alert.AlertType.WARNING, Application.languageResourceBundle.getString(""));
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
