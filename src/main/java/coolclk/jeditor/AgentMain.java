package coolclk.jeditor;

import coolclk.jeditor.api.lang.AutoStoppableThread;
import coolclk.jeditor.api.lang.Stoppable;

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
import java.util.Arrays;
import java.util.Objects;

public class AgentMain {
    public static void premain(String arg, Instrumentation inst) throws UnknownHostException {
        agentmain(arg, inst);
    }

    public static void agentmain(String arg, Instrumentation inst) throws UnknownHostException {
        String[] args = arg.split(" ");
        InetAddress argumentHost = getValueFromArguments(args, "host", null) == null ? InetAddress.getLocalHost() : InetAddress.getByName(getValueFromArguments(arg.split(" "), "host", null));
        int argumentPort = Integer.parseInt(getValueFromArguments(args, "port", "-1"));
        boolean enableLogging = Objects.equals(getValueFromArguments(args, "logging", "-1"), "true");
        PrintStream logger = enableLogging ? System.out : new PrintStream(new OutputStream() { @Override public void write(int b) {  } });
        if (argumentPort > 0 && argumentPort < 0xFFFF) {
            logger.println("JEditor agent loaded and connect to JEditor on " + argumentHost + ":" + argumentPort + ", more see https://coolclk.github.io/JEditor");
            new AutoStoppableThread(new Stoppable() {
                boolean isStop = false;

                @Override
                public void stop() {
                    isStop = true;
                }

                @Override
                public void run() {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(argumentHost, argumentPort));
                        if (socket.isConnected()) {
                            int startInputIndex = 0;
                            byte[] inputBytes;
                            while (!socket.isClosed() && !isStop) {
                                inputBytes = new byte[socket.getInputStream().available() - startInputIndex];
                                if (inputBytes.length > 0) {
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
                                        if (inputArgs.length > 0 && !inputArgs[0].isEmpty()) {
                                            switch (inputArgs[0]) {
                                                case "tree": {
                                                    socket.getOutputStream().write("tree".getBytes(StandardCharsets.UTF_8));
                                                    for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
                                                        socket.getOutputStream().write((" " + loadedClass.getName()).getBytes(StandardCharsets.UTF_8));
                                                    }
                                                    socket.getOutputStream().write(";".getBytes(StandardCharsets.UTF_8));
                                                    socket.getOutputStream().flush();
                                                    break;
                                                }
                                                case "class": {
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
                                                    } catch (UnmodifiableClassException | ClassNotFoundException e) {
                                                        throw new RuntimeException(e);
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
                                                    } catch (ClassNotFoundException | UnmodifiableClassException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("JEditor agent throws an exception when communicate with JEditor", e);
                    }
                }
            }).start();
        }
    }

    static String getValueFromArguments(String[] args, String key, String defaultValue) {
        String keyObj = (key.length() == 1 ? "-" : "--") + key;
        for (String argument : args) {
            if (argument.startsWith(keyObj) && argument.contains("=")) {
                String[] keyAndValue = argument.split("=", 1);
                return keyAndValue[1];
            }
        }
        return defaultValue;
    }
}
