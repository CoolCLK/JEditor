package coolclk.jeditor.editor;

import coolclk.jeditor.Application;
import coolclk.jeditor.api.Configuration;
import coolclk.jeditor.api.javafx.SimpleController;
import coolclk.jeditor.util.ResourceUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;

public class SettingsController extends SimpleController {
    static class SettingsOption {
        private final String value;
        private final String displayName;

        SettingsOption(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public String getValue() {
            return value;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
    
    private final Properties properties = new Properties();
    
    @FXML public TreeView<String> settingsTree;
    @FXML public Pane settingsView;

    @FXML public TreeItem<String> editorGeneral;
    @FXML public TreeItem<String> decompile;
    @Override
    public void initialize(Stage stage) {
        settingsTree.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (Objects.equals(settingsTree.getSelectionModel().getSelectedItem(), editorGeneral)) {
                HBox language = new HBox();
                ChoiceBox<SettingsOption> languageChoiceBox = new ChoiceBox<>();
                for (URL languageProperties : ResourceUtil.listResources("/language")) {
                    String filename = languageProperties.getFile().substring(languageProperties.getFile().lastIndexOf("/") - 1);
                    String code = filename.substring(0, filename.lastIndexOf("."));
                    Properties properties = new Properties();
                    try {
                        properties.load(Application.class.getResourceAsStream("/language/" + filename));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    SettingsOption option = new SettingsOption(code, properties.getProperty(Configuration.ConfigurationKeys.LANGUAGE_NAME, code));
                    languageChoiceBox.getItems().add(option);
                    if (Application.languageResourceBundle.getBaseBundleName().endsWith(code)) {
                        languageChoiceBox.setValue(option);
                    }
                }
                languageChoiceBox.converterProperty().setValue(new StringConverter<SettingsOption>() {
                    @Override
                    public String toString(SettingsOption object) {
                        return object.getDisplayName() + " - " + object.getValue();
                    }

                    @Override
                    public SettingsOption fromString(String string) {
                        return null;
                    }
                });

                Label choiceBoxLabel = new Label(Application.languageResourceBundle.getString("settings.editor.general.language")),
                        restartTip = new Label(Application.languageResourceBundle.getString("settings.editor.general.language.restart"));
                restartTip.setVisible(false);
                restartTip.setTextFill(Color.RED);
                language.getChildren().setAll(choiceBoxLabel, languageChoiceBox, restartTip);
                languageChoiceBox.setOnAction(_event -> {
                    properties.setProperty(Configuration.ConfigurationKeys.GENERAL_LANGUAGE, languageChoiceBox.getValue().getValue());
                    restartTip.setVisible(true);
                });

                language.setAlignment(Pos.CENTER_LEFT);

                HBox fontFamily = new HBox();
                ChoiceBox<String> fontFamilyChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(Font.getFamilies()));
                fontFamilyChoiceBox.setValue(Application.settingsConfiguration.getProperty(Configuration.ConfigurationKeys.GENERAL_FONT_FAMILY, Font.getDefault().getFamily()));
                fontFamilyChoiceBox.converterProperty().setValue(new StringConverter<String>() {
                    @Override
                    public String toString(String object) {
                        return Font.getFontNames(object).get(0);
                    }

                    @Override
                    public String fromString(String string) {
                        return null;
                    }
                });
                Label fontFamilyLabel = new Label(Application.languageResourceBundle.getString("settings.editor.general.fontFamily")),
                        fontFamilyRestartTip = new Label(Application.languageResourceBundle.getString("settings.editor.general.fontFamily.restart"));
                fontFamilyRestartTip.setVisible(false);
                fontFamilyRestartTip.setTextFill(Color.RED);
                fontFamily.getChildren().setAll(fontFamilyLabel, fontFamilyChoiceBox, fontFamilyRestartTip);
                fontFamilyChoiceBox.setOnAction(_event -> {
                    properties.setProperty(Configuration.ConfigurationKeys.GENERAL_FONT_FAMILY, fontFamilyChoiceBox.getValue());
                    fontFamilyRestartTip.setVisible(true);
                });

                settingsView.getChildren().setAll(new VBox(language, fontFamily));
            }
        });
    }

    @FXML
    public void applySettings() throws IOException {
        for (String key : properties.stringPropertyNames()) {
            Application.settingsConfiguration.setProperty(key, properties.getProperty(key));
        }
        Application.settingsConfiguration.saveProperties();
    }

    @FXML
    public void confirmSettings() throws IOException {
        this.applySettings();
        this.close();
    }
}
