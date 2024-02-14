# JEditor

*一个 Java 字节码编辑器，包括反编译和热注入*

---

- [介绍](Introduce.MD)
- 使用
    - [文件反编译](Usage/File-Decompile.MD)
    - [热注入](Usage/Hot-Inject.MD)
- 用户脚本
    - [编写一个用户脚本](UserScript/Create-Coding.MD)
    - [导入脚本至 JEditor](UserScript/Import.MD)
- 开发
    - [JEditor 对 .class 文件的解码及处理](Develop/Class-File-Process.MD)
    - [JEditor 对 .jar 文件的解码及处理](Develop/Jar-File-Process.MD)
    - [JEditor 主线程与 Agent 线程的通信](Develop/Socket-Communication.MD)

---

> 提醒: JEditor 仍在开发中!

### 这是什么

它是一个可以编辑 *.class* 文件和修改正在运行的 JVM 类的编辑器。

### 为什么选择

- 轻松地反编译
- 好的 GUI (*JavaFX*)
- 支持重新定义与重新修改运行中的类 (*可能仅支持JDK 8-14*)
- 用户脚本
- ...

### 如何使用

- 准备 **JDK 8** 或 Java 的更高版本, 你可以在 [这里](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html) 下载到 JDK 8
- 从 [Github Releases](https://github.com/CoolCLK/JEditor/releases) 下载它
- 只需要双击它然后它就应该被打开了
- 若是并非如此, 打开终端 (Windows 是 **cmd.exe**, MacOS 是 **Terminal**, 别告诉我你不会在 Linux 上打开终端), 你只需要在 Windows 上输入像 ``java -jar %userprofile%/Downloads/JEditor-1.0.0.jar`` , 或者像 ``java -jar ~/JEditor-1.0.0.jar`` 这类的, 这取决于你的系统. 如果你不知道 **怎么用 Java 打开一个 JAR 文件**, 事实上你可以 [谷歌一下](https://www.google.com), [必应一下](https://www.bing.com), [百度一下](https://www.baidu.com), [搜狗一下](https://www.sogou.com), [360 SO一下](https://www.so.com).

---

· [简体中文](Introduce.MD) · [English](../../en/documents/Introduce.MD) ·
