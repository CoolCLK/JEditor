package coolclk.jeditor.editor;

import com.sun.tools.attach.VirtualMachine;
import coolclk.jeditor.api.CodeArea;
import coolclk.jeditor.api.SimpleController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sun.jvmstat.monitor.*;

import java.io.File;
import java.util.List;

public class Controller extends SimpleController {
    public TabPane tabs;

    @Override
    public void initialize(Stage stage) {
        CodeArea codeArea = new CodeArea();
        VBox.setVgrow(codeArea, Priority.ALWAYS);
        HBox.setHgrow(codeArea, Priority.ALWAYS);
        tabs.getTabs().add(new Tab("开发者代码测试界面", codeArea));
    }

    @FXML
    public void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(Application.languageResourceBundle.getString("window.openFile.title"));
        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter(Application.languageResourceBundle.getString("window.openFile.filter.jar"), "*.jar"),
                new FileChooser.ExtensionFilter(Application.languageResourceBundle.getString("window.openFile.filter.class"), "*.class")
        );
        List<File> files = fileChooser.showOpenMultipleDialog(this.getStage());
        if (files != null) {
            for (File file : files) {
                ListView<File> fileListView = new ListView<>();
                VBox.setVgrow(fileListView, Priority.ALWAYS);
                CodeArea codeArea = new CodeArea();
                VBox.setVgrow(codeArea, Priority.ALWAYS);
                HBox.setHgrow(codeArea, Priority.ALWAYS);
                Tab tab = new Tab(file.getName(), new HBox(fileListView, codeArea));
                tabs.getTabs().add(tab);
            }
        }
    }

    @FXML
    public void openProcess() {
        ListView<MonitoredVm> processListCell = new ListView<>();
        processListCell.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        processListCell.setItems(getProcesses());
        processListCell.setCellFactory(param -> new ListCell<MonitoredVm>() {
            @Override
            public void updateItem(MonitoredVm item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    try {
                        setText("[" + item.getVmIdentifier().getLocalVmId() + "] " + MonitoredVmUtil.mainClass(item, true));
                    } catch (MonitorException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        Button confirmButton = new Button(Application.languageResourceBundle.getString("ui.confirm")), cancelButton = new Button(Application.languageResourceBundle.getString("ui.cancel"));
        confirmButton.setOnAction(event -> {
            if (processListCell.getSelectionModel().getSelectedItem() != null) {
                try {
                    if (MonitoredVmUtil.isAttachable(processListCell.getSelectionModel().getSelectedItem())) {
                        VirtualMachine virtualMachine = VirtualMachine.attach(String.valueOf(processListCell.getSelectionModel().getSelectedItem().getVmIdentifier().getLocalVmId()));
                        virtualMachine.loadAgent(Application.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "");
                        virtualMachine.detach();
                    } else
                        new Alert(Alert.AlertType.WARNING, Application.languageResourceBundle.getString("window.selectProcess.unAttachable.content"), ButtonType.OK).showAndWait();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else new Alert(Alert.AlertType.INFORMATION, Application.languageResourceBundle.getString("window.selectProcess.nothingSelected.content"), ButtonType.OK).showAndWait();
            cancelButton.getOnAction().handle(new ActionEvent());
        });
        HBox hBox = new HBox(confirmButton, cancelButton);
        VBox vBox = new VBox(processListCell, hBox);
        vBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(vBox, Priority.ALWAYS);
        Stage stage = new Stage(StageStyle.UTILITY);
        stage.setTitle(Application.languageResourceBundle.getString("window.selectProcess.title"));
        cancelButton.setOnAction(event -> stage.close());
        stage.setScene(new Scene(new BorderPane(vBox), 180, 220));
        stage.showAndWait();
    }

    public ObservableList<MonitoredVm> getProcesses() {
        try {
            ObservableList<MonitoredVm> items = FXCollections.observableArrayList();
            MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost("localhost");
            for (final Integer processId : monitoredHost.activeVms()) items.add(monitoredHost.getMonitoredVm(new VmIdentifier("//" + processId)));
            return items;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}