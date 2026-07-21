# Interpreter Design

This document describes the current `jvm-interpreter` design and the target shape for full JVMS bytecode execution. The interpreter is the execution core used by tests, native upcalls, and the GUI debugger.

## Scope

`jvm-interpreter` owns guest bytecode execution for interpreted classes:

- bytecode decode and opcode metadata
- frame-local operand stack and local variable execution
- primitive arithmetic, conversions, comparisons, branches, and returns
- heap-backed object, array, field, and static-field operations
- method invocation, return-value propagation, and guest linkage/runtime errors
- native method dispatch through the layered native resolver
- debugger/event surfaces consumed by `jvm-gui`

Verification, classfile structural parsing, class loading policy, host delegation, and simulated-JNI helpers live in their own modules. The interpreter calls those boundaries; it should not absorb their responsibilities.

## Execution pipeline

```mermaid
sequenceDiagram
    participant Caller as engine/test/gui
    participant Decoder as BytecodeDecoder
    participant Loop as JvmInterpreter
    participant Runtime as jvm-runtime
    participant Native as Native resolver

    Caller->>Loop: execute(code, maxStack, constantPool, heap, locals, hierarchy, statics, nativeMethods)
    Loop->>Decoder: decode(code)
    Decoder-->>Loop: DecodedInstruction list
    loop instructionIndex
        Loop->>Runtime: read/write JvmOperandStack and JvmLocalVariables
        Loop->>Runtime: read/write heap, arrays, fields, static fields
        Loop->>Native: invoke native when resolved method is ACC_NATIVE
        Native-->>Loop: optional return value or guest exception
    end
    Loop-->>Caller: JvmExecutionResult
```

The main loop decodes bytecode once, builds an offset-to-index map for branch targets, then dispatches each instruction by opcode. Branch instructions return the next bytecode offset; ordinary instructions mutate state and advance to the next decoded instruction.

## Runtime state used by the interpreter

| Runtime model | Interpreter use |
| --- | --- |
| `JvmOperandStack` | Per-frame stack with `max_stack` slot-depth enforcement and category-2 width accounting. |
| `JvmLocalVariables` | Per-frame locals with category-2 high-word invalidation and bounds checking. |
| `JvmHeap` | Guest objects, primitive arrays, reference arrays, strings, class mirrors, and Throwable stack-trace payloads. |
| `JvmStaticFields` | Guest static field storage for `getstatic` and `putstatic`. |
| `JvmClassHierarchy` | Assignability, method resolution facts, array-store checks, casts, and `instanceof`. |
| `ConstantPool` | Operand resolution for constants, field refs, method refs, class refs, method handles, and method types. |
| `JvmNativeMethodRegistry` | Native method implementation lookup used by invocation instructions. |

Runtime values are `JvmValue` instances, not host Java objects. Guest exceptions are represented by explicit JVM exception classes or guest error classes so the GUI and tests can distinguish VM behavior from host process failures.

## Opcode execution layout

`OpcodeTable` records metadata and coverage expectations. `BytecodeDecoder` converts bytes into `DecodedInstruction` entries, including offsets and operands. `JvmInterpreter.executeInstruction` dispatches opcode families to focused helpers:

- constants and `ldc`/`ldc_w`/`ldc2_w`
- local load/store and `wide`
- primitive and reference array loads/stores
- stack manipulation (`pop`, `dup*`, `swap`)
- integer/long/float/double arithmetic, shifts, bitwise operations, conversions, and comparisons
- conditional and unconditional branches, `tableswitch`, `lookupswitch`, `jsr`, and `ret`
- field access and static field access
- `invokevirtual`, `invokespecial`, and `invokestatic`
- object and array creation, `arraylength`, `checkcast`, and `instanceof`

Unsupported or not-yet-modeled opcodes must fail with a precise `JvmUnsupportedInstructionException` that includes the mnemonic/opcode and bytecode offset.

## Method invocation

Invocation helpers resolve a constant-pool method reference into a `JvmResolvedMethod` shape. Arguments are popped according to the descriptor, receiver checks are applied for instance calls, and native or interpreted targets are selected by method flags and policy.

The final target behavior is:

1. Interpreted guest method: create a new frame, initialize locals with receiver/arguments, execute until a return instruction, and push any return value to the caller stack.
2. VM intrinsic native: call a whitelisted Kotlin intrinsic when policy allows that class/method.
3. Simulated JNI native: call the simulated JNI layer if no intrinsic exists.
4. Missing native binding: throw guest `java/lang/UnsatisfiedLinkError`.
5. Host-delegated class/method: cross an explicit opaque boundary and emit host delegation events instead of exposing host internals as guest bytecode.

Native upcalls must re-enter interpreted execution through the same invocation semantics rather than calling host reflection for guest classes.

## Exceptions and monitors

Current explicit guest exception classes include arithmetic, null pointer, array bounds, negative array size, array store, class cast, incompatible class change, illegal access, abstract method, and unsatisfied link errors. Full JVMS execution requires exception-table unwinding, precise monitor enter/exit state, synchronized method handling, and stack-trace integration with guest Throwable objects.

Monitor operations are runtime state, not host `synchronized` blocks, so debugger panels and simulated JNI monitor helpers can observe guest monitor events.

## Coverage gates

Interpreter coverage is guarded by:

- `OpcodeTableCoverageTest`: every opcode has table metadata and reserved-opcode classification.
- `OpcodeExecutionCoverageTest`: every non-reserved opcode is classified as implemented, unsupported-by-design, or still pending.
- HotSpot/JVMS corpora: behavioral fixtures compare return values and thrown exceptions as coverage expands.

Adding a new opcode helper should update execution coverage, focused tests, and `docs/spec-coverage.md` together.

## Design invariants

1. Guest application bytecode is interpreted by default.
2. Interpreter state is modeled with `jvm-runtime` guest values and containers, not host JVM objects.
3. Host delegation is explicit and opaque; it is never an accidental fallback for unknown bytecode.
4. Native methods use layered resolution: VM intrinsic first when whitelisted, simulated JNI fallback, then guest `UnsatisfiedLinkError`.
5. Decode/verify/execute responsibilities remain separated even when they reference the same opcode table.
6. GUI stepping consumes snapshots/events; GUI code does not own execution semantics.

## Known gaps / next work

- Full method-frame call stack, class initialization triggers, exception-table unwinding, `invokedynamic`, `invokeinterface`, and complete synchronization semantics remain incremental work.
- Linker and class-loader integration must become authoritative for method/field/class resolution before final conformance.
- Differential fixtures should grow from small opcode cases toward JVMS chapter examples and compiled Java programs.
- Event emission should become complete enough for bytecode stepping, host boundaries, intrinsic calls, simulated JNI calls, and JNI upcall nesting.
