package coolclk.jeditor;

import coolclk.jeditor.api.Configuration;
import coolclk.jeditor.api.javafx.CodeArea;
import coolclk.jeditor.util.FileUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class Application extends javafx.application.Application {
    private final static Logger LOGGER = LogManager.getLogger("main");

    public static Configuration settingsConfiguration;
    public final static ResourceBundle languageResourceBundle;

    static {
        try {
            settingsConfiguration = new Configuration("settings") {
                final File agentVmOptions = new File(this.getPropertiesFile().getParent(), "agent.vmoptions");
                {
                    this.setDefaultProperty(ConfigurationKeys.AGENT_ARGUMENTS, "--port=%port");

                    if (!agentVmOptions.exists() && agentVmOptions.createNewFile()) {
                        LOGGER.debug("Create file \"" + agentVmOptions.getPath() + "\"");
                        FileUtil.write(agentVmOptions, this.getProperty(ConfigurationKeys.AGENT_ARGUMENTS).getBytes(StandardCharsets.UTF_8));
                    }
                }

                public String getProperty(String key) {
                    switch (key) {
                        case Configuration.ConfigurationKeys.AGENT_ARGUMENTS: {
                            return new String(FileUtil.read(agentVmOptions), StandardCharsets.UTF_8);
                        }
                        case Configuration.ConfigurationKeys.GENERAL_FONT_FAMILY: {
                            return Font.getDefault().getFamily();
                        }
                    }
                    return super.getProperty(key);
                }

                public void setProperty(String key, String value) {
                    if (Objects.equals(key, ConfigurationKeys.AGENT_ARGUMENTS)) {
                        FileUtil.write(agentVmOptions, value.getBytes(StandardCharsets.UTF_8));
                    }
                    super.setProperty(key, value);
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String language = settingsConfiguration.getProperty(Configuration.ConfigurationKeys.GENERAL_LANGUAGE, Locale.getDefault().toString().replaceAll("_", "-"));
        ResourceBundle langRes = ResourceBundle.getBundle("language." + language);
        if (Objects.isNull(langRes)) {
            language = "zh-CN";
            langRes = ResourceBundle.getBundle("language.zh-CN");
        }
        settingsConfiguration.setProperty(Configuration.ConfigurationKeys.GENERAL_LANGUAGE, language);
        languageResourceBundle = langRes;
        LOGGER.info(languageResourceBundle.getString("logging.main.languageLoaded"));
    }

    @Override
    public void start(Stage stage) throws IOException {
        Scene scene = new Scene(new FXMLLoader(this.getClass().getResource("/scene/main.fxml"), languageResourceBundle).load());
        scene.getStylesheets().add(Objects.requireNonNull(this.getClass().getResource("/css/code-highlight.css")).toExternalForm());
        applyFont(scene);
        stage.setTitle(languageResourceBundle.getString("window.main.title"));
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        CodeArea.stopExecutors();
        super.stop();
    }

    public static void applyFont(Scene scene) {
        scene.getRoot().setStyle(scene.getRoot().getStyle() + "-fx-font-family: \"" + settingsConfiguration.getProperty(Configuration.ConfigurationKeys.GENERAL_FONT_FAMILY) + "\";");
    }

    public static void main(String[] args) {
        LOGGER.info(languageResourceBundle.getString("logging.main.launch"));
        launch();
    }
}