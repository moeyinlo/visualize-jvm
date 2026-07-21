# Public Engine API

This document records the engine-facing API surface that other modules or external users should treat as the current integration points. It documents the current implementation, not the final JVMS target.

## Intended consumers

- `jvm-gui`: embeds parser/interpreter state in JavaFX views and debugger controls.
- Test/oracle modules: compile fixtures, parse classfiles, and compare behavior.
- Future external library users: import the Pure-Kotlin JVM engine without JavaFX.

## Classfile API

Primary entrypoint:

```kotlin
ClassFileParser.parse(
    bytes: ByteArray,
    source: String = "<memory>",
    attributeParsers: AttributeParserRegistry = AttributeParserRegistry.Empty,
): ClassFile
```

Use this API when a caller already selected bytes from a `.class` file or jar entry. The parser owns classfile decoding and returns a `ClassFile` model with:

- `magic`
- `version`
- `constantPool`
- `accessFlags`
- `identity`
- `fields`
- `methods`
- `attributes`

`source` is part of diagnostics and should name the file, jar entry, or synthetic fixture. Attribute parsing is opt-in through `AttributeParserRegistry`; unknown attributes can remain raw until a caller needs typed data.

Production code must not use ASM as its parser. `jvm-asm-oracle` can use ASM only for fixtures and differential assertions.

## Runtime state API

The reusable runtime state is in `jvm-runtime`:

- `JvmValue` and concrete primitive/reference values model guest stack/local/field values.
- `JvmLocalVariables` models indexed local variable slots and category-2 high-word invalidation.
- `JvmOperandStack` models max-stack constrained operand stack operations.
- `JvmHeap` allocates guest objects, strings, arrays, class mirrors, method handles, and method types.
- `JvmStaticFields` stores prepared static field values and supplies descriptor-based default values.
- `JvmClassHierarchy` resolves already-loaded class definitions for field/method lookup and assignability.
- `JvmMonitorState` models guest monitor ownership and reentrancy.

The current runtime API expects callers to construct a `JvmClassHierarchy` from known `JvmClassDefinition` values. A complete JVMS loader/linker/initializer state machine is still tracked as explicit coverage gaps.

## Interpreter API

Primary entrypoint:

```kotlin
JvmInterpreter.execute(
    code: ByteArray,
    maxStack: Int,
    constantPool: ConstantPool = ConstantPool.fromEntries(emptyList()),
    heap: JvmHeap = JvmHeap(),
    localVariables: JvmLocalVariables = JvmLocalVariables(maxLocals = 0),
    classHierarchy: JvmClassHierarchy = JvmClassHierarchy.Empty,
    staticFields: JvmStaticFields = JvmStaticFields(),
    nativeMethods: JvmNativeMethodRegistry = JvmNativeMethodRegistry.Empty,
    currentClassName: String? = null,
): JvmExecutionResult
```

`JvmExecutionResult` currently exposes the final `operandStack`. Mutable guest state supplied by the caller (`heap`, `localVariables`, `staticFields`, `classHierarchy`, monitors through native context) is the shared execution boundary.

Callers must provide:

- `code`: bytes from a Code attribute or test fixture.
- `maxStack`: the Code attribute `max_stack` for bounded stack operations.
- `constantPool`: required for constant loads, field/method refs, class refs, and dynamic operands.
- `heap`: required for object/array/string/class mirror operations.
- `localVariables`: initialized locals for method arguments and receiver.
- `classHierarchy`: required for strict resolution/access/assignability checks.
- `staticFields`: required for `getstatic`/`putstatic` and static native context.
- `nativeMethods`: layered native resolver.
- `currentClassName`: current guest class for access checks and native context.

## Native method API

Native binding is exposed through `JvmNativeMethodRegistry`:

```kotlin
JvmNativeMethodRegistry.from(
    key to JvmNativeMethodIntrinsic { context, invocation -> ... }
)

JvmNativeMethodRegistry.fromSimulatedJni(
    key to JvmNativeMethodIntrinsic { context, invocation -> ... }
)
```

Resolution order is fixed:

1. VM intrinsic map.
2. Simulated JNI map.
3. Guest `UnsatisfiedLinkError` at invocation if no binding exists.

Keys use `JvmNativeMethodKey(ownerClassName, name, descriptor, isStatic)`. The staticness bit is part of identity so overloaded static/instance native declarations cannot collide.

`JvmNativeMethodContext` gives native implementations access to guest state and controlled helpers:

- `heap`
- `classHierarchy`
- `staticFields`
- `currentClassName`
- monitor/thread/time/stack-trace providers
- `callStaticMethod(...)`
- `callInstanceMethod(...)`

The `call*Method` functions are the simulated-JNI upcall boundary back into guest interpreted execution.

## JNI API

`jvm-jni` exposes a guest-scoped JNI model:

- `JvmJniHandleTable`: local handles for `jobject`, `jclass`, `jmethodID`, and `jfieldID` equivalents.
- `JvmSimulatedJniEnvironment`: `FindClass`, member lookup, field access, strings, arrays, object arrays, and monitor helpers backed by guest state.
- `JvmNativeSymbolNameResolver`: JNI short/long symbol candidate names.
- `JvmNativeLibraryDescriptor`: explicit library export metadata.
- `JvmPanamaDowncallBackend`: adapter-facing data structures for native downcalls.

Simulated JNI helpers must operate on guest heap/class/static/monitor state and must not silently delegate guest object semantics to host JVM objects.

## API stability rules

- Keep JavaFX out of engine APIs.
- Keep ASM out of production APIs.
- Preserve descriptor strings in JVMS form (`I`, `Ljava/lang/String;`, `([I)V`).
- Prefer explicit guest error classes (`JvmNoClassDefFoundError`, `JvmNoSuchMethodError`, `JvmUnsatisfiedLinkError`, etc.) over host exceptions when modeling JVMS behavior.
- Any new public API must document whether it is parser, verifier, runtime, interpreter, host, native, JNI, GUI, or oracle surface.