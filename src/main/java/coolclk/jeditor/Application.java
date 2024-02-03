package coolclk.jeditor;

import coolclk.jeditor.api.javafx.CodeArea;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class Application extends javafx.application.Application {
    public static Configuration settingsConfiguration;
    public final static ResourceBundle languageResourceBundle;

    static {
        try {
            settingsConfiguration = new Configuration("settings");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String language = settingsConfiguration.getProperties().getProperty(Configuration.ConfigurationKeys.GENERAL_LANGUAGE, Locale.getDefault().toString().replaceAll("_", "-"));
        ResourceBundle langRes = ResourceBundle.getBundle("language." + language);
        if (langRes == null) {
            language = "zh-CN";
            langRes = ResourceBundle.getBundle("language.zh-CN");
        }
        settingsConfiguration.getProperties().setProperty(Configuration.ConfigurationKeys.GENERAL_LANGUAGE, language);
        languageResourceBundle = langRes;
    }

    @Override
    public void start(Stage stage) throws IOException {
        Scene scene = new Scene(new FXMLLoader(this.getClass().getResource("/scene/main.fxml"), languageResourceBundle).load());
        scene.getStylesheets().add(Objects.requireNonNull(this.getClass().getResource("/css/code-highlight.css")).toExternalForm());
        stage.setTitle(languageResourceBundle.getString("window.main.title"));
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        CodeArea.stopExecutors();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}