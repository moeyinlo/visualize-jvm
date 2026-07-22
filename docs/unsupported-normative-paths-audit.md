# Unsupported Normative Paths Audit

This audit is derived from `docs/spec-coverage.md` after the Phase 20 documentation pass. It lists the remaining normative surfaces that are not yet implemented or not yet externally differential-tested.

## Ledger status summary

- DIFFERENTIAL: 1
- IMPLEMENTED: 1061
- PENDING: 67

## Pending rows by section

- Chapter 2 - JVM Structure: 4 pending row(s)
- Chapter 4 - Class File Format: 14 pending row(s)
- Chapter 5 - Loading, Linking, and Initializing: 22 pending row(s)
- Chapters 6 and 7 - Instruction Set and Opcode Mnemonics: 22 pending row(s)
- Project-Specific Native and Host Coverage: 5 pending row(s)

## Highest-priority pending normative paths

These are the first pending rows in ledger order and should be converted into implementation commits before the final gate can pass.

| Section | Requirement | Module | Tests |
| --- | --- | --- | --- |
| Chapter 2 - JVM Structure | Exceptions | `jvm-runtime`, `jvm-interpreter` | TBD |
| Chapter 2 - JVM Structure | Instruction set summary | `jvm-interpreter`, `jvm-verifier` | TBD |
| Chapter 2 - JVM Structure | Class libraries interface assumptions | `jvm-host`, `jvm-native`, `jvm-jni` | TBD |
| Chapter 2 - JVM Structure | Public design private implementation boundary | all modules | TBD |
| Chapter 4 - Class File Format | Dynamic constant and invokedynamic runtime resolution | `jvm-runtime` | TBD |
| Chapter 4 - Class File Format | `Code` full structural validation and bytecode instruction constraints | `jvm-classfile` | TBD |
| Chapter 4 - Class File Format | `StackMapTable` verifier semantics | `jvm-verifier` | TBD |
| Chapter 4 - Class File Format | `BootstrapMethods` bootstrap argument resolution semantics | `jvm-runtime` | TBD |
| Chapter 4 - Class File Format | `Module` uniqueness and module relationship constraints | `jvm-classfile`, `jvm-runtime` | TBD |
| Chapter 4 - Class File Format | `ModulePackages` uniqueness constraints | `jvm-classfile`, `jvm-runtime` | TBD |
| Chapter 4 - Class File Format | `NestHost` run-time package and access-control semantics | `jvm-runtime` | TBD |
| Chapter 4 - Class File Format | `NestMembers` mutual-exclusion and access-control semantics | `jvm-classfile`, `jvm-runtime` | TBD |
| Chapter 4 - Class File Format | `PermittedSubclasses` final-class and loading constraints | `jvm-classfile`, `jvm-runtime` | TBD |
| Chapter 4 - Class File Format | Format checking | `jvm-classfile` | TBD |
| Chapter 4 - Class File Format | Static and structural constraints | `jvm-verifier` | TBD |
| Chapter 4 - Class File Format | Verification by type checking | `jvm-verifier` | TBD |
| Chapter 4 - Class File Format | Verification by type inference | `jvm-verifier` | TBD |

## Explicit unsupported-path categories

| Category | Current state | Required follow-up |
| --- | --- | --- |
| Runtime model completeness | Some Chapter 2 runtime structures remain pending, including full frames, dynamic linking, method completion, exceptions, class library assumptions, and startup/termination behavior. | Add runtime models, focused tests, and interpreter integration per ledger row. |
| Class loading/linking/init | Chapter 5 loading, preparation, resolution, access control, method selection, initialization, and VM termination are mostly pending. | Implement class loader/session model, linker errors, initialization triggers, and lifecycle events. |
| Verifier final conformance | Many opcode transfer helpers exist, but full JVMS 4.10 verification by type checking, type inference legacy paths, hierarchy joins, and edge cases remain pending. | Expand verifier rule helpers and HotSpot/malformed fixtures; keep `VerifierRuleCoverageTest` green. |
| Interpreter missing opcodes | The opcode gate still classifies selected opcodes as not yet implemented, especially `invokeinterface`, `invokedynamic`, monitor instructions, exceptions, and some object/control-flow behaviors. | Convert each `OpcodeExecutionCoverage.NotYetImplemented` entry into tested execution or precise spec error behavior. |
| Dynamic constants and invokedynamic | Classfile structures are parsed, but runtime bootstrap resolution and call-site semantics remain pending. | Implement bootstrap method resolution, method handles/types, call-site linkage, and GUI events. |
| Native/JNI completeness | Intrinsics and many data helpers exist, but native-library invocation, pending JNI exceptions, local/global/weak refs, critical sections, direct buffers, and complete upcall families remain pending. | Continue through native resolver and simulated JNI coverage rows. |
| Host delegation | Policy is documented; the host bridge and value marshalling are still pending. | Implement `jvm-host` policy/bridge with observable opaque events and strict default-deny behavior. |
| GUI execution workflow | JavaFX panels exist, but full step-capable engine wiring and all event producers are pending. | Wire execution session snapshots and ordered events into GUI smoke tests. |
| Differential confidence | Some parser/runtime fixtures are differential, but final JVMS behavior is not yet broadly compared against HotSpot. | Grow HotSpot/JVMS chapter corpora and mark rows `DIFFERENTIAL` only after automated comparison. |

## Audit conclusion

The project is not at final JVMS completion yet. Unsupported paths are now explicitly visible in the ledger and summarized here. Final validation proves the current repository state builds and tests, but the overall JVM final gate remains conditional on driving every non-`N/A` row to at least `IMPLEMENTED` and every external-comparison row to `DIFFERENTIAL`.
