package coolclk.jeditor;

import coolclk.jeditor.api.lang.AutoStoppableThread;
import coolclk.jeditor.api.lang.Stoppable;
import coolclk.jeditor.util.ArrayUtil;
import coolclk.jeditor.util.MathUtil;
import coolclk.jeditor.util.StreamUtil;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AgentMain {
    public static void premain(String arg, Instrumentation inst) throws UnknownHostException {
        agentmain(arg, inst);
    }

    public static void agentmain(String arg, Instrumentation inst) throws UnknownHostException {
        String[] args = arg.contains(" ") ? arg.split(" ") : new String[]{arg};
        InetAddress argumentHost = Objects.isNull(getValueFromArguments(args, "host", null)) ? InetAddress.getLocalHost() : InetAddress.getByName(getValueFromArguments(arg.split(" "), "host", null));
        int argumentPort = Integer.parseInt(getValueFromArguments(args, "port", "-1"));
        boolean enableLogging = Objects.equals(getValueFromArguments(args, "logging", "false"), "true");
        PrintStream logger = enableLogging ? System.out : new PrintStream(new OutputStream() { @Override public void write(int b) {  } });
        logger.println("[JEditor] [agentmain/INFO] JEditor agent method loaded with arg \"" + arg + "\"");
        if (argumentPort > 0 && argumentPort < 0xFFFF) {
            new AutoStoppableThread(new Stoppable() {
                boolean isStop = false;

                @Override
                public void stop() {
                    logger.println("[JEditor] [Agent Thread/INFO] Process has been closed, agent thread will be closed");
                    isStop = true;
                }

                @Override
                public void run() {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(argumentHost, argumentPort));
                        Thread.sleep(200);
                        if (socket.isConnected()) {
                            logger.println("[JEditor] [Agent Thread/INFO] Connected to editor on " + argumentHost + ":" + argumentPort);
                            if (socket.isInputShutdown() || socket.isOutputShutdown()) {
                                logger.println("[JEditor] [Agent Thread/ERROR] Editor may close I/O streams");
                                socket.close();
                                return;
                            }
                            final byte[] endStreamFlags = ";".getBytes(); // 结尾字节
                            int streamReadOffset = 0;
                            List<Byte> cacheBytes = new ArrayList<>(); // 数据缓冲区
                            while (!socket.isClosed() && !isStop) {
                                if (socket.getInputStream().available() >= 0 && socket.getInputStream().available() - streamReadOffset >= 0) {
                                    byte[] inputBytes = new byte[socket.getInputStream().available() - streamReadOffset];
                                    if (socket.getInputStream().read(inputBytes) != inputBytes.length) {
                                        logger.println("[JEditor] [NETWORK/WARN] Read data length is different from buffer length, may cause some problems");
                                    }
                                    streamReadOffset += inputBytes.length;
                                    cacheBytes.addAll(Arrays.asList(StreamUtil.bytesToByteArray(inputBytes))); // 写入缓冲区
                                    String inputContents = new String(StreamUtil.byteArrayToBytes(cacheBytes.toArray(new Byte[0])), StandardCharsets.UTF_8);
                                    if (!inputContents.isEmpty() && inputContents.contains(new String(endStreamFlags))) {
                                        boolean needReleaseBuffer = false;
                                        for (String inputContent : inputContents.split(new String(endStreamFlags))) {
                                            if (!inputContent.isEmpty()) {
                                                logger.println("[JEditor] [NETWORK/DEBUG] Input data: " + inputContent);
                                                String[] inputArgs = inputContent.contains(" ") ? inputContent.split(" ") : new String[]{inputContent};
                                                if (inputArgs.length > 0 && !inputArgs[0].isEmpty()) {
                                                    switch (inputArgs[0]) {
                                                        case "close": {
                                                            isStop = true;
                                                            break;
                                                        }
                                                        case "tree": {
                                                            logger.print("[JEditor] [NETWORK/DEBUG] Sending classes...\r");
                                                            StringBuilder data = new StringBuilder("tree");
                                                            for (Class<?> loadedClass : inst.getInitiatedClasses(ClassLoader.getSystemClassLoader())) {
                                                                data.append(" ").append(loadedClass.getName());
                                                                logger.print("[JEditor] [NETWORK/DEBUG] Sending class " + loadedClass + "\r");
                                                            }
                                                            logger.println("[JEditor] [NETWORK/DEBUG] Sent " + inst.getAllLoadedClasses().length + " classes");
                                                            socket.getOutputStream().write(ArrayUtil.connect(data.toString().getBytes(StandardCharsets.UTF_8), endStreamFlags));
                                                            break;
                                                        }
                                                        case "class": {
                                                            if (inputArgs.length == 2) {
                                                                inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
                                                                    if (Objects.equals(className, inputArgs[1])) {
                                                                        try {
                                                                            socket.getOutputStream().write(ArrayUtil.connect(("class " + className + " " + new String(classfileBuffer, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8), endStreamFlags));
                                                                        } catch (IOException e) {
                                                                            new RuntimeException(e).printStackTrace(logger);
                                                                        }
                                                                    }
                                                                    return classfileBuffer;
                                                                }, false);
                                                            }
                                                            break;
                                                        }
                                                        case "retransform": {
                                                            try {
                                                                if (inputArgs.length >= 3) {
                                                                    String targetClassName = inputArgs[1];
                                                                    byte[] newByteCode = String.join(" ", Arrays.asList(inputArgs).subList(2, inputArgs.length)).getBytes(StandardCharsets.UTF_8);
                                                                    ClassFileTransformer transformer = (loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
                                                                        if (Objects.equals(className, targetClassName)) {
                                                                            classfileBuffer = newByteCode;
                                                                        }
                                                                        return classfileBuffer;
                                                                    };
                                                                    inst.addTransformer(transformer, true);
                                                                    inst.retransformClasses(Class.forName(targetClassName));
                                                                    inst.removeTransformer(transformer);
                                                                }
                                                            } catch (UnmodifiableClassException |
                                                                     ClassNotFoundException e) {
                                                                new RuntimeException(e).printStackTrace(logger);
                                                            }
                                                            break;
                                                        }
                                                        case "redefine": {
                                                            try {
                                                                if (inputArgs.length >= 3) {
                                                                    String targetClassName = inputArgs[1];
                                                                    byte[] newByteCode = String.join(" ", Arrays.asList(inputArgs).subList(2, inputArgs.length)).getBytes(StandardCharsets.UTF_8);
                                                                    inst.redefineClasses(new ClassDefinition(Class.forName(targetClassName), newByteCode));
                                                                }
                                                            } catch (ClassNotFoundException |
                                                                     UnmodifiableClassException e) {
                                                                new RuntimeException(e).printStackTrace(logger);
                                                            }
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                            needReleaseBuffer = true;
                                        }
                                        if (needReleaseBuffer) {
                                            cacheBytes.clear();
                                        }
                                    }
                                }
                                socket.getOutputStream().flush();
                            }
                            if (!socket.isClosed()) {
                                socket.close();
                            }
                            logger.println("[JEditor] [Agent Thread/INFO] Editor closed connection, agent thread will be closed");
                        } else {
                            logger.println("[JEditor] [Agent Thread/INFO] No connection, or editor timeout. Agent thread will be closed");
                        }
                    } catch (IOException e) {
                        new RuntimeException("JEditor agent throws an exception when communicate with JEditor", e).printStackTrace(logger);
                    } catch (InterruptedException ignored) {  } finally {
                        logger.println("[JEditor] [Agent Thread/INFO] Agent thread closed");
                    }
                }
            }).start();
        }
        logger.println("[JEditor] [agentmain/INFO] JEditor agent method was closed");
    }

    static String getValueFromArguments(String[] args, String key, String defaultValue) {
        String keyObj = (key.length() == 1 ? "-" : "--") + key;
        for (String argument : args) {
            if (argument.startsWith(keyObj) && argument.contains("=")) {
                String[] keyAndValue = argument.split("=", 2);
                return keyAndValue[1];
            }
        }
        return defaultValue;
    }
}
