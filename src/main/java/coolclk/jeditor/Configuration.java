package coolclk.jeditor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class Configuration {
    public static class ConfigurationKeys {
        public final static String GENERAL_LANGUAGE = "general.language";
    }

    private final File propertiesFile;
    private final Properties properties = new Properties();

    public Configuration(String name) throws IOException {
        propertiesFile = new File(System.getProperty("user.home"), name + ".properties");
        if (!propertiesFile.exists()) {
            propertiesFile.createNewFile();
        }
        this.properties.load(Files.newInputStream(propertiesFile.toPath()));
    }

    public void save() throws IOException {
        this.properties.store(Files.newOutputStream(propertiesFile.toPath()), null);
    }

    public Properties getProperties() {
        return this.properties;
    }
}
