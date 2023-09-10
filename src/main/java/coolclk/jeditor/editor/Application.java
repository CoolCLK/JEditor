package coolclk.jeditor.editor;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Locale;
import java.util.ResourceBundle;

public class Application extends javafx.application.Application {
    public final static ResourceBundle languageResourceBundle = ResourceBundle.getBundle("language." + Locale.getDefault().toString());

    @Override
    public void start(Stage stage) throws IOException {
        Scene scene = new Scene(new FXMLLoader(Application.class.getResource("/scene/main.fxml"), languageResourceBundle).load());
        stage.setTitle(languageResourceBundle.getString("window.main.title"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public static void premain(String arg, Instrumentation inst) {

    }

    public static void agentmain(String arg, Instrumentation inst) {

    }
}