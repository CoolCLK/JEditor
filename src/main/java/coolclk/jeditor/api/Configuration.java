package coolclk.jeditor.api;

import java.io.*;
import java.nio.file.Files;
import java.util.Properties;

/**
 * 配置文件工具
 * @author CoolCLK
 */
public class Configuration {
    public static class ConfigurationKeys {
        public static final String LANGUAGE_NAME = "language.name";

        public final static String GENERAL_LANGUAGE = "general.language";
        public static final String GENERAL_FONT_FAMILY = "general.fontFamily";

        public final static String AGENT_ARGUMENTS = "options.agentArguments";
    }

    private final static File propertiesFolder;
    private final File propertiesFile;
    private Properties properties;
    private Properties defaultProperties = new Properties();

    static {
        propertiesFolder = new File(System.getProperty("user.home"), ".JEditor");
        if (!propertiesFolder.exists()) {
            for (int times = 0; times < 3; times++) {
                if (propertiesFolder.mkdirs()) {
                    break;
                }
            }
        }
    }

    public Configuration(String name) throws IOException {
        this(name + ".properties", false);
    }

    public Configuration(String name, boolean openAsProperties) throws IOException {
        this.propertiesFile = new File(propertiesFolder, name);
        this.setProperties(new Properties());
        for (int times = 0; times < 3; times++) {
            if (this.getPropertiesFile().exists() || this.getPropertiesFile().createNewFile()) {
                if (openAsProperties) {
                    this.properties.load(Files.newInputStream(propertiesFile.toPath()));
                }
                break;
            }
        }
    }

    protected File getPropertiesFile() {
        return this.propertiesFile;
    }

    private Properties getProperties() {
        return this.properties;
    }

    private Properties getDefaultProperties() {
        return this.defaultProperties;
    }

    private void setProperties(Properties pro) {
        this.properties = pro;
    }

    private void setDefaultProperties(Properties pro) {
        this.defaultProperties = pro;
    }

    public void saveProperties() throws IOException {
        this.properties.store(Files.newOutputStream(propertiesFile.toPath()), null);
    }

    public boolean propertyContainsKey(String key) {
        return this.getProperties().containsKey(key);
    }

    public boolean propertyContainsValue(String value) {
        return this.getProperties().containsValue(value);
    }

    public String getProperty(String key) {
        return this.getProperties().getProperty(key, this.defaultProperties.getProperty(key, null));
    }

    public String getProperty(String key, String defaultValue) {
        return this.getProperties().getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        this.getProperties().setProperty(key, value);
    }

    public void setDefaultProperty(String key, String value) {
        this.getDefaultProperties().setProperty(key, value);
    }
}
