# visualize-jvm

可视化JVM执行流程，支持从jar、class文件添加到classpath，完整的类加载流程，支持单步调试标准jvm字节码，支持可视化indy bsm的CallSite绑定。

特色功能：
严格按照specification用Pure-Kotlin实现jvm执行引擎，同时通过panama支持jni。

GUI基于javafx实现，风格参考https://github.com/Col-E/Recaf，没有花里胡哨的设计，以简洁、实用为主

GUI和执行引擎分多模块管理，执行引擎可单独作为library导入到其他项目使用，通过Event Listener interface实现指令级别的event监听