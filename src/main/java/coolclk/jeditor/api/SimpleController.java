package coolclk.jeditor.api;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public abstract class SimpleController implements Initializable {
    private Stage stage;

    public BorderPane root;

    @Override
    public final void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            this.stage = (Stage) root.getScene().getWindow();
            this.initialize(this.stage);
        });
    }

    public abstract void initialize(Stage stage);

    public Stage getStage() {
        return this.stage;
    }

    public void close() {
        this.getStage().close();
    }
}
