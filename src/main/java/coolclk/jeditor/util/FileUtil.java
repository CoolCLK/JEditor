package coolclk.jeditor.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {
    public static byte[] read(Path path) {
        try {
            return StreamUtil.readToEnd(Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] read(File file) {
        return read(file.toPath());
    }

    public static boolean write(Path path, byte[] bytes) {
        try (OutputStream os = Files.newOutputStream(path)) {
            for (byte b : bytes) {
                os.write(b);
            }
            os.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    public static boolean write(File file, byte[] bytes) {
        return write(file.toPath(), bytes);
    }
}
