package coolclk.jeditor.editor;

import coolclk.jeditor.api.CodeArea;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class Application extends javafx.application.Application {
    public final static ResourceBundle languageResourceBundle;

    static {
        ResourceBundle langRes = ResourceBundle.getBundle("language." + Locale.getDefault().toString());
        if (langRes == null) langRes = ResourceBundle.getBundle("language.en_US");
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

    public static void premain(String arg, Instrumentation inst) {

    }

    public static void agentmain(String arg, Instrumentation inst) {

    }
}