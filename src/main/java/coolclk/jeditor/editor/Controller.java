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
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import sun.jvmstat.monitor.*;

import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Controller extends SimpleController {
    protected static class EditorTabInformation {
        File source;
        CodeArea editArea;

        public EditorTabInformation(File file, CodeArea codeArea) {
            this.source = file;
            this.editArea = codeArea;
        }
    }

    public TabPane tabs;

    protected final Map<Tab, EditorTabInformation> tabsInformation = new HashMap<>();

    @Override
    public void initialize(Stage stage) {
        root.addEventFilter(DragEvent.DRAG_OVER, event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
        });
        root.addEventFilter(DragEvent.DRAG_DROPPED, event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                for (File file : dragboard.getFiles()) {
                    processFiles(Collections.singletonList(file));
                }
                success = true;
            }
            event.setDropCompleted(success);
        });
    }

    @FXML
    public void openFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(Application.languageResourceBundle.getString("window.openFile.title"));
        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter(Application.languageResourceBundle.getString("window.openFile.filter.class"), "*.class")
        );
        List<File> files = fileChooser.showOpenMultipleDialog(this.getStage());
        if (files != null) {
            processFiles(files);
        }
    }

    public void processFiles(List<File> files) {
        for (File file : files) {
            switch (file.getName().substring(file.getName().lastIndexOf(".") + 1)) {
                case "class": {
                    try {
                        Printer printer = new Printer() {
                            private static final String TAB = "  ";
                            private static final String NEWLINE = "\n";

                            private int indentationCount = 0;
                            private final StringBuilder sb = new StringBuilder();

                            @Override public String toString() { return sb.toString(); }

                            @Override public void start(int maxLineNumber, int majorVersion, int minorVersion) {}
                            @Override public void end() {}

                            @Override public void printText(String text) { sb.append(text); }
                            @Override public void printNumericConstant(String constant) { sb.append(constant); }
                            @Override public void printStringConstant(String constant, String ownerInternalName) { sb.append(constant); }
                            @Override public void printKeyword(String keyword) { sb.append(keyword); }
                            @Override public void printDeclaration(int type, String internalTypeName, String name, String descriptor) { sb.append(name); }
                            @Override public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) { sb.append(name); }

                            @Override public void indent() { this.indentationCount++; }
                            @Override public void unindent() { this.indentationCount--; }

                            @Override public void startLine(int lineNumber) { for (int i=0; i<indentationCount; i++) sb.append(TAB); }
                            @Override public void endLine() { sb.append(NEWLINE); }
                            @Override public void extraLine(int count) { while (count-- > 0) sb.append(NEWLINE); }

                            @Override public void startMarker(int type) {}
                            @Override public void endMarker(int type) {}
                        };
                        new ClassFileToJavaSourceDecompiler().decompile(new Loader() {
                            @Override
                            public boolean canLoad(String s) {
                                return false;
                            }

                            @Override
                            public byte[] load(String s) {
                                try (FileInputStream fs = new FileInputStream(file)) {
                                    byte[] bytes = new byte[fs.available()];
                                    int b;
                                    int i = 0;
                                    while ((b = fs.read()) != -1) {
                                        bytes[i] = (byte) b;
                                        i++;
                                    }
                                    return bytes;
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }, printer, file.getName());

                        StringBuilder savedContent = new StringBuilder(printer.toString());
                        CodeArea codeArea = new CodeArea(savedContent.toString());
                        VBox.setVgrow(codeArea, Priority.ALWAYS);
                        HBox.setHgrow(codeArea, Priority.ALWAYS);
                        String tabName = file.getName();
                        Tab tab = new Tab(tabName, codeArea);
                        Runnable saveFile = () -> {
                            try {
                                String tmpName = file.getName().substring(0, file.getName().lastIndexOf(".")) + "_JEditor_" + System.currentTimeMillis();
                                File tmpSourceFile = File.createTempFile(tmpName, ".java");
                                FileOutputStream fileOutputStream = new FileOutputStream(tmpSourceFile);
                                fileOutputStream.write(codeArea.getText().getBytes());
                                fileOutputStream.flush();
                                fileOutputStream.close();

                                if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-Xlint:none", tmpSourceFile.getAbsolutePath()) == 0) {
                                    File tmpCompiledFile = new File(tmpSourceFile.getParent() + "/" + tmpName + ".class");
                                    FileInputStream fileInputStream = new FileInputStream(tmpCompiledFile);
                                    byte[] compiledBytecode = new byte[fileInputStream.available()];
                                    int fileInputStreamI = 0, fileInputStreamB;
                                    while ((fileInputStreamB = fileInputStream.read()) != -1) {
                                        compiledBytecode[fileInputStreamI] = (byte) fileInputStreamB;
                                        fileInputStreamI++;
                                    }
                                    fileInputStream.close();

                                    if (!file.exists()) file.createNewFile();
                                    fileOutputStream = new FileOutputStream(file);
                                    fileOutputStream.write(compiledBytecode);
                                    fileOutputStream.flush();
                                    fileOutputStream.close();

                                    savedContent.delete(0, savedContent.length());
                                    savedContent.append(codeArea.getText());
                                }
                            } catch (Exception e) {
                                new Alert(Alert.AlertType.ERROR, Application.languageResourceBundle.getString("window.editor.unWriteable.content"), ButtonType.OK).showAndWait();
                                throw new RuntimeException(e);
                            }
                        };
                        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                            tab.setText(tabName);
                            if (!savedContent.toString().equals(codeArea.getText())) {
                                if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
                                    saveFile.run();
                                } else tab.setText("*" + tabName);
                            }
                        });
                        tab.setOnCloseRequest(event -> {
                            if (!savedContent.toString().equals(codeArea.getText())) {
                                new Alert(Alert.AlertType.INFORMATION, Application.languageResourceBundle.getString("window.editor.askSave.content"), ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(buttonType -> {
                                    if (buttonType == ButtonType.YES) {
                                        saveFile.run();
                                    }
                                });
                            }
                        });
                        tabsInformation.put(tab, new EditorTabInformation(file, codeArea));
                        tabs.getTabs().add(tab);
                    } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, Application.languageResourceBundle.getString("window.editor.unReadable.content"), ButtonType.OK).showAndWait();
                        throw new RuntimeException(e);
                    }
                    break;
                }
                default: {
                    new Alert(Alert.AlertType.INFORMATION, Application.languageResourceBundle.getString("window.openFile.unSupportedFile.content"), ButtonType.OK).showAndWait();
                    break;
                }
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

    public void save() {
        tabs.getSelectionModel().getSelectedItem();
    }

    public void saveAs() {
    }
}