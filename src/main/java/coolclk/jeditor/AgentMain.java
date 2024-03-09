package coolclk.jeditor;

import coolclk.jeditor.api.io.EmptyOutputStream;
import coolclk.jeditor.api.lang.AutoStoppableThread;
import coolclk.jeditor.api.lang.Stoppable;
import coolclk.jeditor.util.ArrayUtil;
import coolclk.jeditor.util.StreamUtil;
import coolclk.jeditor.util.StringUtil;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class AgentMain {
    static class TransformerTask {
        enum TaskType {
            READ,
            WRITE
        }

        private final TaskType type;
        private final String className;
        private byte[] classFileBuffer;

        public TransformerTask(TaskType type, String className) {
            this.type = type;
            this.className = className;
        }

        public TaskType getType() {
            return this.type;
        }

        public String getClassName() {
            return this.className;
        }

        public byte[] getClassFileBuffer() {
            return this.classFileBuffer;
        }

        public void setClassFileBuffer(byte[] buffer) {
            this.classFileBuffer = buffer;
        }

        public void readClass() {}
    }

    /**
     * 当以 javaagent 参数随着其它 JAVA 程序启动时，则调用此方法
     * @throws UnknownHostException 无法获取指定或本地地址时抛出
     * @author CoolCLK
     */
    public static void premain(String arg, Instrumentation inst) throws UnknownHostException {
        agentmain(arg, inst);
    }

    /**
     * 当 JEditor 的 JAR 本体被加载 Agent 时调用此方法
     * @param arg 以 <code>--key=value</code> 的方式输入即可，详见 <a href="https://coolclk.github.io/JEditor/documents">帮助文档</a>
     * @throws UnknownHostException 无法获取指定或本地地址时抛出
     * @author CoolCLK
     */
    public static void agentmain(String arg, Instrumentation inst) throws UnknownHostException {
        String[] args = arg.contains(" ") ? StringUtil.split(arg, " ") : new String[]{arg};
        InetAddress argumentHost = Objects.isNull(getValueFromArguments(args, "host", null)) ? InetAddress.getLocalHost() : InetAddress.getByName(getValueFromArguments(StringUtil.split(arg, " "), "host", null));
        int argumentPort = Integer.parseInt(getValueFromArguments(args, "port", "-1"));
        boolean enableLogging = Objects.equals(getValueFromArguments(args, "logging", "false"), "true");

        PrintStream logger = enableLogging ? System.out : new PrintStream(new EmptyOutputStream());
        logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/agentmain/INFO] JEditor agent method loaded with arg \"" + arg + "\"");
        if (argumentPort > 0 && argumentPort < 0xFFFF) {
            new AutoStoppableThread(new Stoppable() {
                boolean isStop = false;

                @Override
                public void stop() {
                    logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/INFO] Process has been closed, agent thread will be closed");
                    isStop = true;
                }

                @Override
                public void run() {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(argumentHost, argumentPort));
                        Thread.sleep(200);
                        if (socket.isConnected()) {
                            logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/INFO] Connected to editor on " + argumentHost + ":" + argumentPort);

                            List<TransformerTask> transformerTasks = new ArrayList<>();
                            inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
                                AtomicReference<byte[]> buffer = new AtomicReference<>(classfileBuffer);
                                transformerTasks.stream().filter(task -> Objects.equals(task.getClassName(), className)).findAny().ifPresent(task -> {
                                    switch (task.getType()) {
                                        case READ: {
                                            task.setClassFileBuffer(buffer.get());
                                            task.readClass();
                                            break;
                                        }
                                        case WRITE: {
                                            buffer.set(task.getClassFileBuffer());
                                            break;
                                        }
                                    }
                                    transformerTasks.remove(task);
                                });
                                return buffer.get();
                            }, true);
                            logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/INFO] Add the transformer on instrumentation");

                            if (socket.isInputShutdown() || socket.isOutputShutdown()) {
                                logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/ERROR] Editor may close I/O streams");
                                socket.close();
                                return;
                            }
                            final byte[] endStreamFlags = { 0 }; // 结尾字节
                            int streamReadOffset = 0;
                            List<Byte> cacheBytes = new ArrayList<>(); // 数据缓冲区
                            while (!socket.isClosed() && !isStop) {
                                if (socket.getInputStream().available() - streamReadOffset >= 0) {
                                    byte[] inputBytes = new byte[socket.getInputStream().available() - streamReadOffset];
                                    if (socket.getInputStream().read(inputBytes, 0, inputBytes.length) != inputBytes.length) {
                                        logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/NETWORK/WARN] Read data length is different from buffer length, may cause some problems");
                                    }
                                    streamReadOffset += inputBytes.length;
                                    cacheBytes.addAll(Arrays.asList(StreamUtil.bytesToByteArray(inputBytes))); // 写入缓冲区
                                    String inputContents = new String(StreamUtil.byteArrayToBytes(cacheBytes.toArray(new Byte[0])), StandardCharsets.UTF_8);
                                    if (!inputContents.isEmpty() && inputContents.contains(new String(endStreamFlags))) {
                                        for (String inputContent : StringUtil.splitNoLast(inputContents, new String(endStreamFlags, StandardCharsets.UTF_8))) {
                                            if (!inputContent.isEmpty()) {
                                                logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/NETWORK/DEBUG] Input data: " + inputContent);
                                                String[] inputArgs = inputContent.contains(" ") ? StringUtil.split(inputContent, " ") : new String[]{inputContent};
                                                if (inputArgs.length > 0 && !inputArgs[0].isEmpty()) {
                                                    switch (inputArgs[0]) {
                                                        case "close": {
                                                            isStop = true;
                                                            break;
                                                        }
                                                        case "tree": {
                                                            logger.print("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/NETWORK/DEBUG] Sending classes...\r");
                                                            for (Class<?> loadedClass : inst.getInitiatedClasses(ClassLoader.getSystemClassLoader())) {
                                                                socket.getOutputStream().write(ArrayUtil.connect(("tree " + loadedClass.getName()).getBytes(StandardCharsets.UTF_8), endStreamFlags));
                                                                logger.print("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/NETWORK/DEBUG] Sending class " + loadedClass + "\r");
                                                            }
                                                            logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/NETWORK/DEBUG] Sent " + inst.getAllLoadedClasses().length + " classes");
                                                            break;
                                                        }
                                                        case "class": {
                                                            if (inputArgs.length == 2) {
                                                                TransformerTask task = new TransformerTask(TransformerTask.TaskType.READ, inputArgs[1]) {
                                                                    @Override
                                                                    public void readClass() {
                                                                        try {
                                                                            socket.getOutputStream().write(ArrayUtil.connect(("class " + getClassName() + " buffer " + new String(getClassFileBuffer(), StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8), endStreamFlags));
                                                                        } catch (IOException e) {
                                                                            new RuntimeException(e).printStackTrace(logger);
                                                                        }
                                                                        logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/INFO] Sent class " + getClassName() + " file buffer");
                                                                    }
                                                                };
                                                                transformerTasks.add(task);
                                                                try {
                                                                    Class<?> target = Class.forName(inputArgs[1]);
                                                                    if (inst.isModifiableClass(target)) {
                                                                        inst.retransformClasses(target);
                                                                    } else {
                                                                        socket.getOutputStream().write(ArrayUtil.connect(("class " + inputArgs[1] + " modifiable false").getBytes(StandardCharsets.UTF_8), endStreamFlags));
                                                                    }
                                                                } catch (ClassNotFoundException ignored) {
                                                                    logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/ERROR] Unknown class " + inputArgs[1]);
                                                                } catch (UnmodifiableClassException e) {
                                                                    new RuntimeException("Unexpected exception, please create a new issues to solve it", e).printStackTrace(logger);
                                                                }
                                                            } else {
                                                                logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/WARN] Incomplete command: " + inputContent);
                                                            }
                                                            break;
                                                        }
                                                        case "retransform": {
                                                            if (inputArgs.length >= 3) {
                                                                TransformerTask task = new TransformerTask(TransformerTask.TaskType.READ, inputArgs[1]);
                                                                task.setClassFileBuffer(String.join(" ", Arrays.asList(inputArgs).subList(2, inputArgs.length)).getBytes(StandardCharsets.UTF_8));
                                                                transformerTasks.add(task);
                                                                try {
                                                                    inst.retransformClasses(Class.forName(inputArgs[1]));
                                                                } catch (UnmodifiableClassException ignored) {
                                                                    logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/ERROR] The class " + inputArgs[1] + " is unmodifiable");
                                                                } catch (ClassNotFoundException ignored) {
                                                                    logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/ERROR] Unknown class " + inputArgs[1]);
                                                                }
                                                            }
                                                            break;
                                                        }
                                                        case "redefine": {
                                                            if (inputArgs.length >= 3) {
                                                                String targetClassName = inputArgs[1];
                                                                byte[] newByteCode = String.join(" ", Arrays.asList(inputArgs).subList(2, inputArgs.length)).getBytes(StandardCharsets.UTF_8);
                                                                try {
                                                                        inst.redefineClasses(new ClassDefinition(Class.forName(targetClassName), newByteCode));
                                                                } catch (UnmodifiableClassException ignored) {
                                                                    logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/ERROR] The class " + inputArgs[1] + " is unmodifiable");
                                                                } catch (ClassNotFoundException ignored) {
                                                                    logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/ERROR] Unknown class " + inputArgs[1]);
                                                                }
                                                            }
                                                            break;
                                                        }
                                                        default: {
                                                            logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/WARN] Unknown command: " + inputArgs[0]);
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                            cacheBytes.removeAll(Arrays.asList(StreamUtil.bytesToByteArray(inputContent.getBytes(StandardCharsets.UTF_8))));
                                            cacheBytes.removeAll(Arrays.asList(StreamUtil.bytesToByteArray(endStreamFlags)));
                                        }
                                    }
                                }
                                socket.getOutputStream().flush();
                            }
                            if (!socket.isClosed()) {
                                socket.close();
                            }
                            logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/INFO] Editor closed connection, agent thread will be closed");
                        } else {
                            logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/INFO] No connection, or editor timeout. Agent thread will be closed");
                        }
                    } catch (IOException e) {
                        new RuntimeException("JEditor agent throws an exception when communicate with JEditor", e).printStackTrace(logger);
                    } catch (InterruptedException ignored) {  } finally {
                        logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/Agent Thread/INFO] Agent thread closed");
                    }
                }
            }).start();
        }
        logger.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") + "] [JEditor/agentmain/INFO] JEditor agent method was closed");
    }

    static String getValueFromArguments(String[] args, String key, String defaultValue) {
        String keyObj = (key.length() == 1 ? "-" : "--") + key;
        for (String argument : args) {
            if (argument.startsWith(keyObj) && argument.contains("=")) {
                String[] keyAndValue = StringUtil.split(argument, "=", 2);
                return keyAndValue[1];
            }
        }
        return defaultValue;
    }
}
