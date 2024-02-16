package coolclk.jeditor.editor;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import coolclk.jeditor.Application;
import coolclk.jeditor.api.Configuration;
import coolclk.jeditor.api.javafx.CodeArea;
import coolclk.jeditor.api.javafx.SimpleController;
import coolclk.jeditor.api.lang.AutoStoppableThread;
import coolclk.jeditor.api.lang.Stoppable;
import coolclk.jeditor.util.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.jvmstat.monitor.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Controller extends SimpleController {
    private final Logger LOGGER = LogManager.getLogger("Main Controller");

    @FXML public Menu userScriptsMenu;
    @Override
    public void initialize(Stage stage) {
        root.addEventFilter(DragEvent.DRAG_OVER, event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
        });
        LOGGER.debug(StringUtil.replaceAll(Application.languageResourceBundle.getString("logging.controller.initialize"), "{title}", stage.getTitle()));
    }

    @FXML public TabPane tabs;
    @FXML
    public void openProcess() {
        ListView<MonitoredVm> processListCell = new ListView<>();
        processListCell.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        try {
            ObservableList<MonitoredVm> items = FXCollections.observableArrayList();
            MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost("localhost");
            for (final Integer processId : monitoredHost.activeVms()) items.add(monitoredHost.getMonitoredVm(new VmIdentifier("//" + processId)));
            processListCell.setItems(items);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            final Logger _LOGGER = LogManager.getLogger("Agent Worker");
            if (processListCell.getSelectionModel().getSelectedItem() != null) {
                try {
                    if (MonitoredVmUtil.isAttachable(processListCell.getSelectionModel().getSelectedItem())) {
                        int socketPort;
                        do {
                            socketPort = new Random().nextInt(0xFFFF);
                        } while (SocketUtil.isPortUsed(socketPort));

                        TreeView<String> classesTreeView = new TreeView<>();
                        VBox.setVgrow(classesTreeView, Priority.ALWAYS);

                        TabPane editTabPane = new TabPane();
                        Map<Tab, CodeArea> editTabs = new HashMap<>();
                        VBox.setVgrow(editTabPane, Priority.ALWAYS);

                        SplitPane tabSpiltPane = new SplitPane();
                        tabSpiltPane.getItems().addAll(classesTreeView, editTabPane);
                        tabSpiltPane.setDividerPositions(0.25);
                        VBox.setVgrow(tabSpiltPane, Priority.ALWAYS);
                        HBox.setHgrow(tabSpiltPane, Priority.ALWAYS);

                        HBox tabContent = new HBox(tabSpiltPane);

                        String tabName;
                        try {
                            tabName = "[" + processListCell.getSelectionModel().getSelectedItem().getVmIdentifier().getLocalVmId() + "] " + MonitoredVmUtil.mainClass(processListCell.getSelectionModel().getSelectedItem(), true);
                        } catch (MonitorException e) {
                            throw new RuntimeException(e);
                        }
                        Tab tab = new Tab(tabName, tabContent);
                        ContextMenu tabRightClickMenu = new ContextMenu();
                        tabRightClickMenu.hide();
                        AtomicBoolean socketClosed = new AtomicBoolean(false);

                        tab.setOnCloseRequest(_event -> {
                            AtomicBoolean needSave = new AtomicBoolean(false);
                            editTabs.values().forEach(codeArea -> {
                                if (codeArea.isTextChanged()) {
                                    needSave.set(true);
                                }
                            });
                            if (needSave.get()) {
                                editTabs.values().forEach(codeArea -> {
                                    if (codeArea.isTextChanged()) {
                                        new Alert(Alert.AlertType.INFORMATION, Application.languageResourceBundle.getString("window.editor.askSave.content"), ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(buttonType -> {
                                            if (buttonType == ButtonType.YES) {
                                                if (codeArea.isTextChanged()) {
                                                    codeArea.save(true);
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                            socketClosed.set(true);
                        });

                        tabs.getTabs().add(tab);

                        File agentJarFile = new File(Application.class.getProtectionDomain().getCodeSource().getLocation().getFile());

                        if (agentJarFile.exists() && agentJarFile.isFile()) {
                            int finalSocketPort = socketPort;
                            InetAddress socketAddress = InetAddress.getLocalHost();

                            new AutoStoppableThread(new Stoppable() {
                                boolean isStop = false;
                                @Override
                                public void stop() {
                                    isStop = true;
                                }

                                @Override
                                public void run() {
                                    final byte[] endStreamFlags = ";".getBytes(); // 结尾字节

                                    try (final ServerSocket serverSocket = new ServerSocket()) {
                                        serverSocket.bind(new InetSocketAddress(socketAddress, finalSocketPort));
                                        serverSocket.setSoTimeout(5000);

                                        Socket socket = serverSocket.accept();
                                        if (socket.isInputShutdown() || socket.isOutputShutdown()) {
                                            socketClosed.set(true);
                                            Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, Application.languageResourceBundle.getString("window.selectProcess.agentClosedIO.content"), ButtonType.OK).showAndWait());
                                            return;
                                        }

                                        int streamReadOffset = 0;
                                        List<Byte> cacheBytes = new ArrayList<>(); // 数据缓冲区
                                        socket.getOutputStream().write(ArrayUtil.connect("tree".getBytes(StandardCharsets.UTF_8), endStreamFlags));
                                        socket.getOutputStream().flush();
                                        while (!socket.isClosed() && !isStop) {
                                            if (socketClosed.get()) {
                                                _LOGGER.info(Application.languageResourceBundle.getString("logging.agentWorker.closingSocket"));
                                                socket.getOutputStream().write(ArrayUtil.connect("close".getBytes(StandardCharsets.UTF_8), endStreamFlags));
                                                socket.close();
                                                serverSocket.close();
                                                break;
                                            }
                                            if (socket.getInputStream().available() - streamReadOffset >= 0) {
                                                byte[] inputBytes = new byte[socket.getInputStream().available() - streamReadOffset];
                                                if (inputBytes.length != socket.getInputStream().read(inputBytes)) {
                                                    _LOGGER.warn(Application.languageResourceBundle.getString("logging.agentWorker.lengthNotMatch"));
                                                }
                                                streamReadOffset += inputBytes.length;
                                                cacheBytes.addAll(Arrays.asList(StreamUtil.bytesToByteArray(inputBytes))); // 写入缓冲区
                                                String inputContents = new String(StreamUtil.byteArrayToBytes(cacheBytes.toArray(new Byte[0])), StandardCharsets.UTF_8);
                                                if (!inputContents.isEmpty() && inputContents.contains(new String(endStreamFlags))) {
                                                    for (String inputContent : StringUtil.split(inputContents, new String(endStreamFlags))) {
                                                        if (!inputContent.isEmpty()) {
                                                            _LOGGER.debug(StringUtil.replaceAll(Application.languageResourceBundle.getString("logging.agentWorker.acceptData"), "{data}", inputContent));
                                                            String[] inputArgs = inputContent.contains(" ") ? StringUtil.split(inputContent, " ") : new String[]{inputContent};
                                                            if (inputArgs.length > 0 && !inputArgs[0].isEmpty()) {
                                                                switch (inputArgs[0]) {
                                                                    case "tree": {
                                                                        final Runnable freshTreeView = () -> {
                                                                            classesTreeView.getRoot().getChildren().clear();
                                                                            List<String> classesName = Arrays.asList(inputArgs).subList(1, inputArgs.length);
                                                                            for (final String className : classesName) {
                                                                                if (!(className.startsWith("[") || className.startsWith("[["))) {
                                                                                    Platform.runLater(() -> {
                                                                                        ObservableList<TreeItem<String>> treeParent = classesTreeView.getRoot().getChildren();
                                                                                        for (String classParent : className.contains(".") ? StringUtil.split(className, ".") : new String[]{className}) {
                                                                                            TreeItem<String> newParent = treeParent.stream().filter(parent -> Objects.equals(parent.getValue(), classParent)).findAny().orElse(new TreeItem<>(classParent));
                                                                                            if (!treeParent.contains(newParent)) {
                                                                                                if (Objects.equals(className.substring(className.lastIndexOf(".") + 1), classParent)) {
                                                                                                    classesTreeView.addEventFilter(MouseEvent.MOUSE_CLICKED, _event -> {
                                                                                                        if (_event.getClickCount() == 2 && Objects.equals(classesTreeView.getSelectionModel().getSelectedItem(), newParent)) {
                                                                                                            Tab editTab = new Tab(className);
                                                                                                            CodeArea editCodeArea = new CodeArea() {
                                                                                                                @Override
                                                                                                                public void save(boolean save) {
                                                                                                                    if (save) {
                                                                                                                        String data = "";
                                                                                                                        try {
                                                                                                                            data += (VersionUtil.javaVersionCompare(((StringMonitor) processListCell.getSelectionModel().getSelectedItem().findByName("java.property.java.version")).stringValue(), "1.5") > 0) ? "retransform" : "redefine";
                                                                                                                        } catch (
                                                                                                                                MonitorException e) {
                                                                                                                            throw new RuntimeException(e);
                                                                                                                        }
                                                                                                                        data += " " + classParent + " " + this.getText();
                                                                                                                        try {
                                                                                                                            socket.getOutputStream().write(ArrayUtil.connect(data.getBytes(StandardCharsets.UTF_8), endStreamFlags));
                                                                                                                            super.save(true);
                                                                                                                        } catch (
                                                                                                                                IOException e) {
                                                                                                                            throw new RuntimeException(e);
                                                                                                                        }
                                                                                                                    } else {
                                                                                                                        editTab.setText("*" + classParent);
                                                                                                                    }
                                                                                                                }
                                                                                                            };
                                                                                                            editCodeArea.setEditable(false);
                                                                                                            try {
                                                                                                                socket.getOutputStream().write(ArrayUtil.connect(("class " + className).getBytes(StandardCharsets.UTF_8), endStreamFlags));
                                                                                                            } catch (
                                                                                                                    IOException e) {
                                                                                                                throw new RuntimeException(e);
                                                                                                            }
                                                                                                            editTab.setContent(editCodeArea);
                                                                                                            editTabs.put(editTab, editCodeArea);
                                                                                                            editTabPane.getTabs().add(editTab);
                                                                                                            editTab.setOnCloseRequest(event -> {
                                                                                                                if (editCodeArea.isTextChanged()) {
                                                                                                                    new Alert(Alert.AlertType.INFORMATION, Application.languageResourceBundle.getString("window.editor.askSave.content"), ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(buttonType -> {
                                                                                                                        if (buttonType == ButtonType.YES) {
                                                                                                                            if (editCodeArea.isTextChanged()) {
                                                                                                                                editCodeArea.save(true);
                                                                                                                            }
                                                                                                                        }
                                                                                                                    });
                                                                                                                }
                                                                                                            });
                                                                                                        }
                                                                                                    });
                                                                                                }
                                                                                                treeParent.add(newParent);
                                                                                            }
                                                                                            treeParent = newParent.getChildren();
                                                                                        }
                                                                                    });
                                                                                }
                                                                                _LOGGER.debug(StringUtil.replaceAll(Application.languageResourceBundle.getString("logging.agentWorker.tree.processedClass"), "{className}", className));
                                                                            }
                                                                            _LOGGER.debug(StringUtil.replaceAll(Application.languageResourceBundle.getString("logging.agentWorker.tree.succeedProcessedClasses"), "{length}", classesName.size()));
                                                                        };
                                                                        Platform.runLater(() -> {
                                                                            if (Objects.isNull(classesTreeView.getRoot())) {
                                                                                TreeItem<String> root = new TreeItem<>(Application.languageResourceBundle.getString("tab.agent.treeView.root.value"));
                                                                                classesTreeView.addEventFilter(MouseEvent.MOUSE_CLICKED, _event -> {
                                                                                    if (_event.getButton() == MouseButton.SECONDARY) {
                                                                                        tabRightClickMenu.getItems().clear();
                                                                                        MenuItem addClassMenuItem = new MenuItem(Application.languageResourceBundle.getString("tab.agent.treeView.menu.addClass")),
                                                                                                importClassMenuItem = new MenuItem(Application.languageResourceBundle.getString("tab.agent.treeView.menu.importClass"));
                                                                                        importClassMenuItem.setOnAction(event -> {
                                                                                            FileChooser fileChooser = new FileChooser();
                                                                                            fileChooser.setTitle(Application.languageResourceBundle.getString("tab.agent.treeView.root.menu.importClass.fileChooser.title"));
                                                                                            fileChooser.getExtensionFilters().addAll(
                                                                                                    new FileChooser.ExtensionFilter(Application.languageResourceBundle.getString("tab.agent.treeView.root.menu.importClass.fileChooser.extensionDescription.class"), "*.class"),
                                                                                                    new FileChooser.ExtensionFilter(Application.languageResourceBundle.getString("tab.agent.treeView.root.menu.importClass.fileChooser.extensionDescription.java"), "*.java")
                                                                                            );
                                                                                            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(getStage());
                                                                                            if (selectedFiles != null) {
                                                                                                for (File file : selectedFiles) {
                                                                                                    String fileExtension = file.getName().substring(file.getName().lastIndexOf(".")).toLowerCase();
                                                                                                    switch (fileExtension) {
                                                                                                        case ".class":
                                                                                                        case ".java": {
                                                                                                            try {
                                                                                                                String data = "redefine ";
                                                                                                                List<String> classNames = new ArrayList<>();
                                                                                                                TreeItem<String> nowClassItem = classesTreeView.getSelectionModel().getSelectedItem();
                                                                                                                while (nowClassItem.getParent() != classesTreeView.getRoot()) {
                                                                                                                    classNames.add(0, nowClassItem.getValue());
                                                                                                                    nowClassItem = nowClassItem.getParent();
                                                                                                                }
                                                                                                                data += String.join(".", classNames) + "." + file.getName().substring(0, file.getName().lastIndexOf(".")) + " ";
                                                                                                                if (Objects.equals(fileExtension, ".java")) {
                                                                                                                    data += new String(BytecodeUtil.fromCode(new String(FileUtil.read(file))), StandardCharsets.UTF_8);
                                                                                                                } else {
                                                                                                                    data += new String(FileUtil.read(file), StandardCharsets.UTF_8);
                                                                                                                }
                                                                                                                socket.getOutputStream().write(ArrayUtil.connect(data.getBytes(StandardCharsets.UTF_8), endStreamFlags));
                                                                                                            } catch (IOException e) {
                                                                                                                throw new RuntimeException(e);
                                                                                                            }
                                                                                                            break;
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        });
                                                                                        tabRightClickMenu.getItems().addAll(addClassMenuItem, importClassMenuItem);
                                                                                        if (classesTreeView.getSelectionModel().getSelectedItem() == root) {
                                                                                            MenuItem refreshMenuItem = new MenuItem(Application.languageResourceBundle.getString("tab.agent.treeView.root.menu.refresh"));
                                                                                            refreshMenuItem.setOnAction(event -> {
                                                                                                try {
                                                                                                    socket.getOutputStream().write(ArrayUtil.connect("tree".getBytes(StandardCharsets.UTF_8), endStreamFlags));
                                                                                                } catch (
                                                                                                        IOException e) {
                                                                                                    throw new RuntimeException(e);
                                                                                                }
                                                                                            });
                                                                                            tabRightClickMenu.getItems().addAll(new SeparatorMenuItem(), refreshMenuItem);
                                                                                        }
                                                                                        tabRightClickMenu.show(_event.getPickResult().getIntersectedNode(), Side.BOTTOM, 0, 0);
                                                                                    }
                                                                                });
                                                                                classesTreeView.setRoot(root);
                                                                            }
                                                                            freshTreeView.run();
                                                                        });
                                                                        break;
                                                                    }
                                                                    case "class": {
                                                                        if (inputArgs.length >= 3) {
                                                                            for (Tab _tab : editTabs.keySet()) {
                                                                                if (tab.getText().endsWith(inputArgs[1])) {
                                                                                    editTabs.get(_tab).clear();
                                                                                    editTabs.get(_tab).appendText(BytecodeUtil.toCode(String.join(" ", Arrays.asList(inputArgs).subList(2, inputArgs.length)).getBytes(StandardCharsets.UTF_8)));
                                                                                    editTabs.get(_tab).setEditable(true);
                                                                                    break;
                                                                                }
                                                                            }
                                                                        }
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    cacheBytes.clear();
                                                }
                                            }
                                            socket.getOutputStream().flush();
                                        }
                                    } catch (SocketTimeoutException e) {
                                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, Application.languageResourceBundle.getString("window.selectProcess.agentNoResponse.content"), ButtonType.OK).showAndWait());
                                    } catch (IOException e) {
                                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, Application.languageResourceBundle.getString("window.selectProcess.communicateWithAgentError.content"), ButtonType.OK).showAndWait());
                                        throw new RuntimeException("When connecting agent process throw an exception", e);
                                    } finally {
                                        Platform.runLater(() -> tabs.getTabs().remove(tab));
                                    }
                                }
                            }).start();

                            try {
                                VirtualMachine virtualMachine = VirtualMachine.attach(String.valueOf(processListCell.getSelectionModel().getSelectedItem().getVmIdentifier().getLocalVmId()));
                                virtualMachine.loadAgent(agentJarFile.getAbsolutePath(), StringUtil.replaceAll(Application.settingsConfiguration.getProperty(Configuration.ConfigurationKeys.AGENT_ARGUMENTS), "%port", String.valueOf(socketPort), "%host", socketAddress.toString()));
                                virtualMachine.detach();
                            } catch (AgentLoadException | AttachNotSupportedException |
                                     AgentInitializationException | IOException e) {
                                new Alert(Alert.AlertType.ERROR, Application.languageResourceBundle.getString("window.selectProcess.loadAgentError.content"), ButtonType.OK).showAndWait();
                                throw new RuntimeException("Load agent to process error", e);
                            }
                        } else {
                            new Alert(Alert.AlertType.ERROR, Application.languageResourceBundle.getString("window.selectProcess.agentJarNotFound.content"), ButtonType.OK).showAndWait();
                            Platform.runLater(() -> tabs.getTabs().remove(tab));
                        }
                    } else {
                        new Alert(Alert.AlertType.WARNING, Application.languageResourceBundle.getString("window.selectProcess.unAttachable.content"), ButtonType.OK).showAndWait();
                    }
                } catch (UnknownHostException e) {
                    _LOGGER.error(Application.languageResourceBundle.getString("logging.agentWorker.unknownHost"), e);
                } catch (MonitorException e) {
                    _LOGGER.error(Application.languageResourceBundle.getString("logging.agentWorker.checkAttachableError"), e);
                }
            } else {
                new Alert(Alert.AlertType.INFORMATION, Application.languageResourceBundle.getString("window.selectProcess.nothingSelected.content"), ButtonType.OK).showAndWait();
            }
            cancelButton.getOnAction().handle(new ActionEvent());
        });
        HBox hBox = new HBox(confirmButton, cancelButton);
        VBox vBox = new VBox(processListCell, hBox);
        vBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(vBox, Priority.ALWAYS);
        Stage stage = new Stage();
        stage.setTitle(Application.languageResourceBundle.getString("window.selectProcess.title"));
        cancelButton.setOnAction(event -> stage.close());
        Scene scene = new Scene(new BorderPane(vBox), 220, 220);
        Application.applyFont(scene);
        stage.setScene(scene);
        stage.showAndWait();
    }

    @FXML public Stage settingsStage = null;
    @FXML
    public void openSettings() throws IOException {
        if (Objects.isNull(settingsStage)) {
            settingsStage = new Stage();
            Scene scene = new Scene(new FXMLLoader(Application.class.getResource("/scene/settings.fxml"), Application.languageResourceBundle).load());
            Application.applyFont(scene);
            settingsStage.setScene(scene);
            settingsStage.setTitle(Application.languageResourceBundle.getString("window.settings.title"));
            settingsStage.setOnCloseRequest(event -> settingsStage = null);
            settingsStage.showAndWait();
            return;
        }
        settingsStage.toFront();
    }

    @FXML
    public void save() {
        tabs.getSelectionModel().getSelectedItem();
    }

    @FXML
    public void saveAs() {
    }

    @FXML
    public void openHelpDocuments() {
        DesktopUtil.openUrl("https://coolclk.github.io/JEditor/" + Application.settingsConfiguration.getProperty(Configuration.ConfigurationKeys.GENERAL_LANGUAGE, "en") + "/documents/Introduce.html");
    }

    private Stage editAgentArgumentsStage = null;
    @FXML
    public void editAgentArguments() {
        if (Objects.isNull(editAgentArgumentsStage)) {
            editAgentArgumentsStage = new Stage();
            BorderPane root = new BorderPane();
            TextArea textArea = new TextArea(Application.settingsConfiguration.getProperty(Configuration.ConfigurationKeys.AGENT_ARGUMENTS));
            HBox.setHgrow(textArea, Priority.ALWAYS);
            VBox.setVgrow(textArea, Priority.ALWAYS);
            Label tipLabel = new Label(Application.languageResourceBundle.getString("window.editAgentArguments.reminder"));
            VBox editArea = new VBox(textArea, tipLabel);
            HBox.setHgrow(editArea, Priority.ALWAYS);
            VBox.setVgrow(editArea, Priority.ALWAYS);
            root.setCenter(editArea);
            Button confirmButton = new Button(Application.languageResourceBundle.getString("ui.confirm")),
                    cancelButton = new Button(Application.languageResourceBundle.getString("ui.cancel"));
            confirmButton.setOnAction(event -> {
                cancelButton.getOnAction().handle(null);
                Application.settingsConfiguration.setProperty(Configuration.ConfigurationKeys.AGENT_ARGUMENTS, textArea.getText());
            });
            cancelButton.setOnAction(event -> {
                editAgentArgumentsStage.close();
                editAgentArgumentsStage = null;
            });
            HBox buttonBar = new HBox(confirmButton, cancelButton);
            buttonBar.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(buttonBar, Priority.ALWAYS);
            root.setBottom(buttonBar);
            Scene scene = new Scene(root);
            Application.applyFont(scene);
            editAgentArgumentsStage.setScene(scene);
            editAgentArgumentsStage.setHeight(150);
            editAgentArgumentsStage.setWidth(250);
            editAgentArgumentsStage.setResizable(false);
            editAgentArgumentsStage.setTitle(Application.languageResourceBundle.getString("window.editAgentArguments.title"));
            editAgentArgumentsStage.setOnCloseRequest(event -> editAgentArgumentsStage = null);
            editAgentArgumentsStage.showAndWait();
            return;
        }
        editAgentArgumentsStage.toFront();
    }

    @FXML
    public void checkUpdate() {
    }

    private Stage aboutStage = null;
    @FXML
    public void openAbout() throws IOException {
        if (Objects.isNull(aboutStage)) {
            aboutStage = new Stage();
            Scene scene = new Scene(new FXMLLoader(Application.class.getResource("/scene/about.fxml"), Application.languageResourceBundle).load());
            Application.applyFont(scene);
            aboutStage.setScene(scene);
            aboutStage.setResizable(false);
            aboutStage.setTitle(Application.languageResourceBundle.getString("window.about.title"));
            aboutStage.setOnCloseRequest(event -> aboutStage = null);
            aboutStage.showAndWait();
            return;
        }
        aboutStage.toFront();
    }
}