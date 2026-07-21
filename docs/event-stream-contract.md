# Event Stream Contract

The event stream is the observable boundary between JVM engine execution and the JavaFX GUI. Engine modules own VM semantics; GUI modules render immutable event snapshots into list/table models.

## Contract principles

1. Events are append-only observations, not commands.
2. Every event has a monotonically increasing `sequence: Long` within one run/debug session.
3. Events use JVMS internal names (`java/lang/String`) and descriptors (`(I)V`) rather than Java source syntax where bytecode identity matters.
4. GUI views consume immutable snapshot DTOs and format them into user-facing items.
5. Host-delegated calls are opaque boundaries; simulated JNI and guest upcalls remain visible as guest-scoped events.
6. If an engine subsystem is incomplete, its event family may exist as a GUI contract before the engine emits it for every path.

## Current event families

| Family | Snapshot | Required fields | Semantics |
| --- | --- | --- | --- |
| Class loading | `ClassLoadingEventSnapshot` | `sequence`, `loader`, `className`, `source` | A loader obtained a class definition or host-delegated class boundary. |
| Linking | `LinkingEventSnapshot` | `sequence`, `className`, `phase`, `target` | Verification, preparation, or resolution activity for a class/link target. |
| Exception unwinding | `ExceptionUnwindingEventSnapshot` | `sequence`, `throwableClassName`, `action`, `frame`, `bytecodeOffset` | Throwable creation, frame unwinding, handler match, or uncaught exit. |
| Host delegation | `HostDelegationEventSnapshot` | `sequence`, `action`, `policy`, `className`, `methodName`, `descriptor`, `detail` | Decision/result/failure at the opaque host JVM boundary. |
| Simulated JNI | `SimulatedJniCallSnapshot` | `sequence`, `action`, `functionName`, `localFrameDepth`, `arguments`, `result`, `pendingException` | Guest-scoped JNIEnv helper entry/return/failure/pending-exception state. |

Additional GUI event panels exist for invokedynamic/condy, monitor operations, native intrinsic frames, and JNI upcall nesting. They follow the same rule: immutable snapshot in, deterministic display model out.

## Ordering and identity

- `sequence` is global within a run so panels can be correlated even when each panel filters a single event family.
- A class is identified by `(loader, className)` once real loader identity is implemented. Current partial surfaces may use `loader = "bootstrap"`, `"app"`, or policy names.
- A method is identified by `(ownerClassName, methodName, descriptor, isStatic)` when native or invocation identity matters.
- A field is identified by `(ownerClassName, name, descriptor)`.
- Bytecode locations use `bytecodeOffset`/BCI, not instruction index.

## Required lifecycle coverage

A complete run should eventually emit events for:

1. Class lookup/loading/derivation.
2. Link phases: verification, preparation, resolution.
3. Initialization scheduling, `<clinit>` entry/return/failure, and erroneous state transitions.
4. Method frame entry/return/throw.
5. Opcode-level stepping state: PC, locals, operand stack, heap/static deltas.
6. Field/method/class resolution success and failure.
7. Monitor enter/exit and ownership failures.
8. Exceptions: throw site, matched handler, unwound frames, uncaught errors.
9. Native resolution: intrinsic lookup, simulated JNI fallback, unresolved native errors.
10. Simulated JNI calls and guest upcalls.
11. Host delegation decisions, returns, and failures.
12. Dynamic constants and call site bootstrap activity.

## GUI rendering contract

Each event view has the same shape:

```kotlin
data class SomeEventSnapshot(...)
data class SomeEventItem(val sequence: Long, val text: String)
data class SomeEventsModel(val items: List<SomeEventItem>)
```

`fromEvents` or `fromCalls` converts snapshots into deterministic strings. Tests assert both empty-list behavior and representative formatting. This keeps GUI rendering stable while the engine event producers evolve.

## Compatibility rules

- Additive fields are preferred over changing existing field meanings.
- If an event needs richer machine-readable detail, add structured fields and keep `detail`/`text` as display-only.
- Do not encode host-only object identities as guest object identities.
- Do not collapse simulated JNI upcalls into a single native event; nested guest execution must remain visible.
- Do not treat GUI event models as authoritative engine state. Engine state remains in runtime/interpreter/JNI modules.