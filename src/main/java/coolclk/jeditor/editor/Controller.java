package coolclk.jeditor.editor;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import coolclk.jeditor.Application;
import coolclk.jeditor.api.CodeArea;
import coolclk.jeditor.api.SimpleController;
import coolclk.jeditor.util.SocketUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sun.jvmstat.monitor.*;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class Controller extends SimpleController {
    public TabPane tabs;

    @Override
    public void initialize(Stage stage) {
        root.addEventFilter(DragEvent.DRAG_OVER, event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
        });
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
                        int socketPort;
                        do {
                            socketPort = new Random().nextInt(0xFFFF);
                        } while (SocketUtil.isPortUsed(socketPort));

                        TreeView<Button> classesTreeView = new TreeView<>();
                        VBox.setVgrow(classesTreeView, Priority.ALWAYS);

                        CodeArea codeArea = new CodeArea("");
                        VBox.setVgrow(codeArea, Priority.ALWAYS);

                        SplitPane tabSpiltPane = new SplitPane();
                        tabSpiltPane.getItems().addAll(classesTreeView, codeArea);
                        tabSpiltPane.setDividerPositions(0.25);
                        VBox.setVgrow(tabSpiltPane, Priority.ALWAYS);
                        HBox.setHgrow(tabSpiltPane, Priority.ALWAYS);

                        HBox tabContent = new HBox(tabSpiltPane);

                        String tabName = "";
                        try {
                            tabName = "[" + processListCell.getSelectionModel().getSelectedItem().getVmIdentifier().getLocalVmId() + "] " + MonitoredVmUtil.mainClass(processListCell.getSelectionModel().getSelectedItem(), true);
                        } catch (MonitorException e) {
                            throw new RuntimeException(e);
                        }
                        Tab tab = new Tab(tabName, tabContent);

                        /*
                        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, _event -> {
                            tab.setText(tabName);
                            if (!savedContent.toString().equals(codeArea.getText())) {
                                if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(_event)) {
                                    saveFile.run();
                                } else tab.setText("*" + tabName);
                            }
                        });
                        tab.setOnCloseRequest(_event -> {
                            if (!savedContent.toString().equals(codeArea.getText())) {
                                new Alert(Alert.AlertType.INFORMATION, Application.languageResourceBundle.getString("window.editor.askSave.content"), ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(buttonType -> {
                                    if (buttonType == ButtonType.YES) {
                                        saveFile.run();
                                    }
                                });
                            }
                        });
                        */

                        tabs.getTabs().add(tab);

                            File agentJarFile = new File(Application.class.getProtectionDomain().getCodeSource().getLocation().getFile());

                            if (agentJarFile.exists() && agentJarFile.isFile()) {
                                int finalSocketPort = socketPort;

                                new Thread(() -> {
                                    try (final ServerSocket serverSocket = new ServerSocket()) {
                                        serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), finalSocketPort));
                                        serverSocket.setSoTimeout(5000);

                                        Socket socket = serverSocket.accept();
                                        socket.getOutputStream().write("tree;".getBytes(StandardCharsets.UTF_8));
                                        socket.getOutputStream().flush();

                                        int startInputIndex = 0;
                                        byte[] inputBytes;
                                        while (!socket.isClosed()) {
                                            inputBytes = new byte[socket.getInputStream().available() - startInputIndex];
                                            int inputReadIndex = 0;
                                            int inputReadByte;
                                            while ((inputReadByte = socket.getInputStream().read()) > -1) {
                                                inputBytes[inputReadIndex] = (byte) inputReadByte;
                                                inputReadIndex++;
                                            }
                                            String inputContent = new String(inputBytes, StandardCharsets.UTF_8);
                                            if (!inputContent.isEmpty()) {
                                                if (inputContent.endsWith(";")) {
                                                    startInputIndex += inputReadIndex;
                                                    inputContent = inputContent.substring(0, inputContent.length() - 1);
                                                }

                                                String[] inputArgs = inputContent.contains(" ") ? inputContent.split(" ") : new String[]{inputContent};
                                                switch (inputArgs[0]) {
                                                    case "tree": {
                                                        for (String className : Arrays.asList(inputArgs).subList(1, inputArgs.length)) {
                                                            String[] classParents = className.contains(".") ? className.split("\\.") : new String[]{className};
                                                            ObservableList<TreeItem<Button>> treeParent = classesTreeView.getEditingItem().getChildren();
                                                            for (String classParent : classParents) {
                                                                TreeItem<Button> parent = treeParent.stream().filter(item -> Objects.equals(item.getValue().getText(), classParent)).findAny().orElse(null);
                                                                if (parent == null) {
                                                                    parent = new TreeItem<>(new Button(classParent));
                                                                    treeParent.add(parent);
                                                                }
                                                                treeParent = parent.getChildren();
                                                            }
                                                        }
                                                        break;
                                                    }
                                                    case "alert": {
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    } catch (SocketTimeoutException e) {
                                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, Application.languageResourceBundle.getString("window.selectProcess.agentNoResponse.content"), ButtonType.OK).showAndWait());
                                    } catch (IOException e) {
                                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, Application.languageResourceBundle.getString("window.selectProcess.communicateWithAgentError.content"), ButtonType.OK).showAndWait());
                                        throw new RuntimeException("When connecting agent process throw an exception", e);
                                    } finally {
                                        tabs.getTabs().remove(tab);
                                    }
                                }).start();

                                try {
                                    VirtualMachine virtualMachine = VirtualMachine.attach(String.valueOf(processListCell.getSelectionModel().getSelectedItem().getVmIdentifier().getLocalVmId()));
                                    virtualMachine.loadAgent(agentJarFile.getAbsolutePath(), "--port " + socketPort);
                                    virtualMachine.detach();
                                } catch (AgentLoadException | AttachNotSupportedException |
                                         AgentInitializationException | IOException e) {
                                    new Alert(Alert.AlertType.ERROR, Application.languageResourceBundle.getString("window.selectProcess.loadAgentError.content"), ButtonType.OK).showAndWait();
                                    throw new RuntimeException("Load agent to process error", e);
                                }
                            } else {
                                new Alert(Alert.AlertType.ERROR, Application.languageResourceBundle.getString("window.selectProcess.agentJarNotFound.content"), ButtonType.OK).showAndWait();
                            }
                    } else
                        new Alert(Alert.AlertType.WARNING, Application.languageResourceBundle.getString("window.selectProcess.unAttachable.content"), ButtonType.OK).showAndWait();
                } catch (MonitorException e) {
                    throw new RuntimeException("When get process detail error", e);
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

    public void save() {
        tabs.getSelectionModel().getSelectedItem();
    }

    public void saveAs() {
    }
}