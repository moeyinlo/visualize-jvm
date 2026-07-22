# visualize-jvm

visualize-jvm is a modular JVM implementation and execution visualizer written in Kotlin. It follows the Java Virtual Machine Specification and separates class-file parsing, bytecode verification, runtime state, interpretation, host integration, and native/JNI support into focused modules.

The JavaFX desktop interface can load JAR and class files and is designed for inspecting JVM behavior at the instruction level, including class loading, bytecode stepping, and `invokedynamic` bootstrap and `CallSite` binding. The execution engine is independent of the GUI, so it can also be embedded as a library in other projects and observed through instruction-level events.
