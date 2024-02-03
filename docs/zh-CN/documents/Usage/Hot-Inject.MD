# JEditor 文档

### 热注入

---

- [介绍](../Introduce.MD)
- 使用
    - [文件反编译](File-Decompile.MD)
    - [热注入](Hot-Inject.MD)
- 用户脚本
    - [编写一个用户脚本](../UserScript/Create-Coding.MD)
    - [导入脚本至 JEditor](../UserScript/Import.MD)
- 开发
    - [JEditor 对 .class 文件的解码及处理](../Develop/Class-File-Process.MD)
    - [JEditor 对 .jar 文件的解码及处理](../Develop/Jar-File-Process.MD)
    - [JEditor 主线程与 Agent 线程的通信](../Develop/Socket-Communication.MD)

---

### 支持的进程

JEditor 使用 ``sun.jvmstat.monitor.MonitoredHost.getMonitoredHost(java.lang.String)`` 方法来获取 JVM 进程列表

因其特殊性, JEditor 将 JDK 8 中的 ``tools.jar`` 作为依赖使用

故可能 JEditor 目前**仅支持调试使用 JDK 8 运行的 JVM 进程**

请勿遗忘在目标进程 JAR 包中 ``META-INF/MAINFEST.MF`` 中添加属性:

``` manifest
Can-Redefine-Classes: true
Can-Retransform-Classes: true

```

### 使用步骤

1. 在 **菜单栏** 找到 **文件** , 唤出菜单, 选择 **打开进程**
2. 在唤出的窗口选择想调试的 JVM 进程后, 点击 **确认** , 否则 **取消** 或直接关闭选择进程窗口
3. 在加载 Agent 文件时, JEditor 可能会有稍许无响应时间 (一般不超过 5s), 请稍作等待
4. 在加载完毕后, JEditor 会在 Tab 栏创建一个新 Tab , 若一切正常则应在其左侧的 Class 树中正常显示目标 JVM 进程已加载的所有类
5. 若是加载失败并弹出窗口, 可参考下表处理 (可能消息会与实际有部分差异)
6. 若直接抛出错误崩溃, 请先自行判断后再询问他人, 最终解决方案为提交 [Github Issues](https://github.com/CoolCLK/JEditor/issues/new)

| 错误信息            | 错误分析                                                                                                   | 解决方案                                                                                                   |
|-----------------|--------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| 没有进程被选中         | 还未选择进程                                                                                                 | 选择进程即可                                                                                                 |
| 目标进程暂不支持热调试     | 目标进程 JDK 版本与 JEditor 运行 JDK 版本不一致                                                                      | 切换 JDK 运行 JEditor                                                                                      |
| 无法找到热调试 JAR 文件  | JEditor 获取自身 JAR 文件时发现自身文件不存在 (可能运行时被清除)                                                               | 若是在压缩包中打开, 请先解压其压缩包. 若是您使用 IDE 正在调试 JEditor, 请先构建后运行.                                                  |
| 调试进程无响应         | JEditor 创建 ``java.net.ServerSocket`` 后, 在指定时间内无连接, 认为 Agent 进程没有连接到 JEditor, 推断 Agent 进程无响应            | 在 **菜单栏** -> **帮助** -> **诊断工具** -> **编辑 Agent 参数...** 中添加 ``--logging true`` 后再次选择目标进程并检查目标进程输出或检查网络设置 |
| 与调试进程通信时发生错误    | JEditor 连接到 Agent 进程后出错                                                                                | 一般是目标进程出错, 若问题仍旧存在请提交 [Github Issues](https://github.com/CoolCLK/JEditor/issues/new)                   |
| 加载热调试 JAR 文件时错误 | JEditor 使用方法 ``com.sun.tools.attach.VirtualMachine.loadAgent(java.lang.String, java.lang.String)`` 时报错 | 更换 JEditor 或 JDK 版本, 若问题仍旧存在请提交 [Github Issues](https://github.com/CoolCLK/JEditor/issues/new)         |