package coolclk.jeditor.api.javafx;

import javafx.application.Platform;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 基础的 {@link javafx.fxml.Initializable} 又添加了可获取、操作的 {@link javafx.stage.Stage} （需拥有 root 根节点）
 * @author CoolCLK
 */
public abstract class SimpleController implements Initializable {
    private Stage stage;

    @FXML public Node root;

    @Override
    public final void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            this.stage = (Stage) (root.getScene().getWindow());
            this.initialize(this.stage);
        });
    }

    public abstract void initialize(Stage stage);

    public Stage getStage() {
        return this.stage;
    }

    public void close() {
        if (this.getStage().getOnCloseRequest() != null) {
            this.getStage().getOnCloseRequest().handle(new WindowEvent(this.getStage(), EventType.ROOT));
        }
        this.getStage().close();
    }
}
