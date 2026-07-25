# visualize-jvm

visualize-jvm is a modular JVM implementation and execution visualizer written in Kotlin. It follows the Java Virtual Machine Specification and is designed to make JVM behavior observable while keeping the execution engine reusable outside the desktop interface.

## Highlights

- Load JAR and class files into a project classpath and inspect classes, members, constant pools, and bytecode.
- Step through bytecode instructions while viewing the current frame, local variables, and operand stack.
- Observe class loading, linking, initialization, exception unwinding, monitor activity, and dynamic linkage events.
- Inspect `invokedynamic` bootstrap behavior and `CallSite` binding.
- Model host delegation, native intrinsics, and simulated JNI calls through explicit execution boundaries.

## Architecture

The core engine is divided into modules for class-file parsing, verification, runtime state, and bytecode interpretation. Separate modules handle host interaction, native execution, simulated JNI, the JavaFX interface, and ASM-based reference checks. The engine can be embedded as a library and observed through instruction-level events without depending on the GUI.
