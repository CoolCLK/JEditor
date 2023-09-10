package coolclk.jeditor.editor;

import com.sun.tools.attach.VirtualMachine;
import coolclk.jeditor.api.SimpleController;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;

import java.util.ArrayList;
import java.util.List;

public class Controller extends SimpleController {
    public Menu processSelectMenu;

    @Override
    public void initialize(Stage stage) {

    }

    @FXML
    public void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(Application.languageResourceBundle.getString("window.openFile.title"));
        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter(Application.languageResourceBundle.getString("window.openFile.filter.jar"), "*.jar"),
                new FileChooser.ExtensionFilter(Application.languageResourceBundle.getString("window.openFile.filter.class"), "*.class")
        );
        fileChooser.showOpenMultipleDialog(this.getStage());
    }

    @FXML
    public void refreshProcessMenu() {
        try {
            List<MenuItem> items = new ArrayList<>();
            MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost("localhost");
            for (final Integer processId : monitoredHost.activeVms()) {
                MenuItem item = new MenuItem("[" + processId + "] " + MonitoredVmUtil.mainClass(monitoredHost.getMonitoredVm(new VmIdentifier("//" + processId)), true));
                item.setOnAction(event -> {
                    try {
                        VirtualMachine virtualMachine = VirtualMachine.attach(String.valueOf(processId));
                        virtualMachine.loadAgent(Application.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "");
                        virtualMachine.detach();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                items.add(item);
            }
            processSelectMenu.getItems().setAll(items);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}