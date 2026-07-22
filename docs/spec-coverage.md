# JVMS 26 Coverage Ledger

This ledger is the required source of truth for implementing Java SE 26 JVM Specification coverage in visualize-jvm.

Status values:
- `PENDING`: not implemented yet.
- `IMPLEMENTED`: code exists and focused tests pass.
- `DIFFERENTIAL`: implementation also has HotSpot, `javap`, or ASM-oracle comparison where applicable.
- `N/A`: explicitly not a JVMS requirement for this project.

Every commit that implements a JVMS item must update the matching row with implementation and test references.

## Chapter 2 - JVM Structure

| JVMS | Requirement | Module | Tests | Status |
| --- | --- | --- | --- | --- |
| 2.1 | Class file format connection to runtime | `jvm-classfile`, `jvm-runtime` | TBD | PENDING |
| 2.2 | Data types | `jvm-runtime` | TBD | PENDING |
| 2.3 | Primitive types and values | `jvm-runtime` | TBD | PENDING |
| 2.3 | Primitive value runtime model and JVM slot categories | `jvm-runtime` | `JvmPrimitiveValueTest` | IMPLEMENTED |
| 2.3.1 | Integral types and values | `jvm-runtime` | TBD | PENDING |
| 2.3.2 | Floating-point types and values | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.3.3 | `returnAddress` runtime value model stores non-negative bytecode targets as category-one JVM values | `jvm-runtime` | `JvmReturnAddressValueTest` | IMPLEMENTED |
| 2.3.4 | `boolean` runtime value model stores true/false as category-one JVM values | `jvm-runtime` | `JvmPrimitiveValueTest` | IMPLEMENTED |
| 2.4 | Reference types and values | `jvm-runtime` | TBD | PENDING |
| 2.4 | Null and non-null reference runtime value model | `jvm-runtime` | `JvmReferenceValueTest` | IMPLEMENTED |
| 2.5.1 | pc register model stores Java bytecode offsets and explicit native-method undefined state | `jvm-runtime` | `JvmProgramCounterTest` | IMPLEMENTED |
| 2.5.2 | Per-thread JVM stack model stores frames in LIFO order and reports configured stack overflow or empty-stack underflow | `jvm-runtime` | `JvmThreadStackTest` | IMPLEMENTED |
| 2.5.3 | Heap stores guest objects and arrays behind opaque references with VM-owned payload and field state | `jvm-runtime` | `JvmHeapTest` | IMPLEMENTED |
| 2.5.3, 2.7 | Guest heap object allocation with opaque positive object references | `jvm-runtime` | `JvmHeapTest` | IMPLEMENTED |
| 2.5.3, 2.7 | Guest `java/lang/String` heap objects retain VM-owned string payloads | `jvm-runtime` | `JvmHeapTest` | IMPLEMENTED |
| 2.5.3, 2.7 | Guest `java/lang/Class` mirror objects retain the represented class name and are interned by name | `jvm-runtime` | `JvmHeapTest` | IMPLEMENTED |
| 2.5.4 | Method area stores per-class runtime metadata, static field storage, duplicate definition checks, and hierarchy views | `jvm-runtime` | `JvmMethodAreaTest` | IMPLEMENTED |
| 2.5.5 | Run-time constant pool stores per-class one-based literal and symbolic entries plus resolved constant cache | `jvm-runtime` | `JvmRuntimeConstantPoolTest` | IMPLEMENTED |
| 2.5.6 | Native method stack model stores native frames with guest method identity, entry point metadata, execution environment, and overflow/underflow behavior | `jvm-native` | `JvmNativeMethodStackTest` | IMPLEMENTED |
| 2.6 | Execution frame model binds a resolved method to local variables, operand stack, runtime constant pool, and pc state | `jvm-runtime` | `JvmExecutionFrameTest` | IMPLEMENTED |
| 2.6.1 | Local variable arrays are frame-scoped slot arrays used by interpreter load/store instructions with category-one and category-two slot rules | `jvm-runtime`, `jvm-interpreter` | `JvmLocalVariablesTest`, `JvmInterpreterTest` | IMPLEMENTED |
| 2.6.1 | Runtime local variable array stores category-one values and category-two values in consecutive slots with lower-index addressing | `jvm-runtime` | `JvmLocalVariablesTest` | IMPLEMENTED |
| 2.6.2 | Operand stacks are frame-scoped LIFO slot stacks used by interpreter execution with category-one/category-two depth accounting | `jvm-runtime`, `jvm-interpreter` | `JvmOperandStackTest`, `JvmInterpreterTest` | IMPLEMENTED |
| 2.6.2 | Runtime operand stack LIFO, slot depth, underflow, and `max_stack` bounds | `jvm-runtime` | `JvmOperandStackTest` | IMPLEMENTED |
| 2.6.3 | Dynamic linking resolves runtime constant pool symbolic class, field, method, literal, and string entries and caches resolved constants | `jvm-runtime` | `JvmRuntimeDynamicLinkerTest` | IMPLEMENTED |
| 2.6.4 | Normal method invocation completion | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.6.5 | Abrupt method invocation completion | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.7 | Representation of objects | `jvm-runtime` | TBD | PENDING |
| 2.8 | Floating-point arithmetic | `jvm-interpreter` | TBD | PENDING |
| 2.9.1 | Instance initialization methods | `jvm-runtime`, `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| 2.9.2 | Class initialization methods | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.9.3 | Signature polymorphic methods | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.11.10 | Guest monitor ownership state tracks object monitor owner thread and reentrant hold count | `jvm-runtime` | `JvmMonitorStateTest` | IMPLEMENTED |
| 2.11.10 | Guest monitor wait-set state releases ownership for wait and records FIFO notification targets | `jvm-runtime` | `JvmMonitorStateTest.*waitForNotification*`, `JvmMonitorStateTest.*notify*` | IMPLEMENTED |
| 2.10 | Exceptions | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.11 | Instruction set summary | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| 2.12 | Class libraries interface assumptions | `jvm-host`, `jvm-native`, `jvm-jni` | TBD | PENDING |
| 2.13 | Public design private implementation boundary | all modules | TBD | PENDING |

## Chapter 4 - Class File Format

| JVMS | Requirement | Module | Tests | Status |
| --- | --- | --- | --- | --- |
| 4.1 | `ClassFile` full structure after header | `jvm-classfile` | `ClassFileParserTest` | IMPLEMENTED |
| 4.1 | `ClassFile` magic and Java SE 26 version range | `jvm-classfile` | `ClassFileHeaderParserTest` | IMPLEMENTED |
| 4.1 | `ClassFile` writer skeleton for magic and version header | `jvm-classfile` | `ClassFileWriterTest` | IMPLEMENTED |
| 4.1 | `ClassFile` full writer structure | `jvm-classfile` | `ClassFileWriterTest` | IMPLEMENTED |
| 4.1 | `ClassFile` javac fixture byte-for-byte round trip | `jvm-classfile`, `jvm-asm-oracle` | `ClassFileRoundTripTest` | IMPLEMENTED |
| 4.1 | `ClassFile` core structure agrees with `javap -v` | `jvm-classfile`, `jvm-asm-oracle` | `ClassFileJavapDifferentialTest` | DIFFERENTIAL |
| 4.1 | `constant_pool_count` and constant pool slot table | `jvm-classfile` | `ConstantPoolParserTest` | IMPLEMENTED |
| 4.1 | Constant pool writer preserves table order and two-slot gaps | `jvm-classfile` | `ConstantPoolWriterTest` | IMPLEMENTED |
| 4.1 | Class access flags and legal class/interface/module combinations | `jvm-classfile` | `ClassAccessFlagsTest` | IMPLEMENTED |
| 4.1 | `this_class`, `super_class`, and interfaces table | `jvm-classfile` | `ClassIdentityParserTest` | IMPLEMENTED |
| 4.2.1 | Binary class and interface names in internal form | `jvm-classfile` | `ConstantPoolReferenceValidationTest` | IMPLEMENTED |
| 4.2.1 | Array class names in `CONSTANT_Class_info` | `jvm-classfile` | `ConstantPoolReferenceValidationTest` | IMPLEMENTED |
| 4.2.2 | Generic unqualified name syntax | `jvm-classfile` | `ConstantPoolReferenceValidationTest` | IMPLEMENTED |
| 4.2.2 | Method-specific `<init>` and `<clinit>` name constraints | `jvm-classfile`, `jvm-verifier` | `ConstantPoolReferenceValidationTest` | IMPLEMENTED |
| 4.2.3 | Module and package names | `jvm-classfile` | `ConstantPoolReferenceValidationTest` | IMPLEMENTED |
| 4.3.2 | Field descriptors | `jvm-classfile` | `ConstantPoolReferenceValidationTest` | IMPLEMENTED |
| 4.3.3 | Method descriptors | `jvm-classfile` | `ConstantPoolReferenceValidationTest` | IMPLEMENTED |
| 4.4.1 | `CONSTANT_Class_info` | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.4.2 | Field, method, interface method refs | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.4.3 | `CONSTANT_String_info` | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.4.4 | Integer and float constants | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.4.5 | Long and double constants | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.4.6 | `CONSTANT_NameAndType_info` | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.4.7 | `CONSTANT_Utf8_info` modified UTF-8 | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.4.8 | `CONSTANT_MethodHandle_info` | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.4.9 | `CONSTANT_MethodType_info` | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.4.10 | `CONSTANT_Dynamic_info`, `CONSTANT_InvokeDynamic_info` classfile structures | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.4.10 | Dynamic constant and invokedynamic runtime resolution | `jvm-runtime` | TBD | PENDING |
| 4.4.11 | `CONSTANT_Module_info` | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.4.12 | `CONSTANT_Package_info` | `jvm-classfile` | `ConstantPoolEntryParserTest` | IMPLEMENTED |
| 4.5 | Fields full validation | `jvm-classfile` | `FieldInfoParserTest` | IMPLEMENTED |
| 4.5 | `fields_count` and `field_info` raw structure | `jvm-classfile` | `FieldInfoParserTest` | IMPLEMENTED |
| 4.5 | `fields_count` and empty-attribute `field_info` writer structure | `jvm-classfile` | `MemberInfoWriterTest` | IMPLEMENTED |
| 4.6 | Methods full validation | `jvm-classfile` | `MethodInfoParserTest` | IMPLEMENTED |
| 4.6 | `methods_count` and `method_info` raw structure | `jvm-classfile` | `MethodInfoParserTest` | IMPLEMENTED |
| 4.6 | `methods_count` and empty-attribute `method_info` writer structure | `jvm-classfile` | `MemberInfoWriterTest` | IMPLEMENTED |
| 4.7 | `attribute_info` name/length structure and parser dispatch by `CONSTANT_Utf8_info` name | `jvm-classfile` | `AttributeParserRegistryTest` | IMPLEMENTED |
| 4.7 | Raw `attribute_info` writer for preserved bytes | `jvm-classfile` | `AttributeInfoWriterTest`, `MemberInfoWriterTest` | IMPLEMENTED |
| 4.7.1 | Unknown and user-defined attributes | `jvm-classfile` | `AttributeParserRegistryTest` | IMPLEMENTED |
| 4.7.1 | Unknown and user-defined attribute writer preserves bytes | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.2 | `ConstantValue` | `jvm-classfile` | `ConstantValueAttributeParserTest` | IMPLEMENTED |
| 4.7.2 | `ConstantValue` writer | `jvm-classfile` | `AttributeInfoWriterTest`, `MemberInfoWriterTest` | IMPLEMENTED |
| 4.7.3 | `Code` full structural validation and bytecode instruction constraints | `jvm-classfile` | TBD | PENDING |
| 4.7.3 | `Code` method attribute presence and cardinality for concrete, native, abstract, and initialization methods | `jvm-classfile` | `MethodInfoParserTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` bytecode instruction layout, switch shape, reserved opcode, and branch target static constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` class-reference instruction constant-pool operands and array-dimension static constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` `newarray` primitive array `atype` static constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` `ldc`, `ldc_w`, and `ldc2_w` loadable constant-pool operand constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` field access instruction `CONSTANT_Fieldref` operand constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` `invokevirtual` `CONSTANT_Methodref` operand constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` `invokespecial` and `invokestatic` version-specific method reference operand constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` `invokeinterface` interface method reference, count, and zero-byte operand constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` `invokeinterface` descriptor-derived `count` operand constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` `invokedynamic` dynamic call site and zero-byte operand constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` direct method invocation special method name constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` `jsr`, `jsr_w`, and `ret` class file version 51.0+ exclusion constraints | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` category-1 local variable instruction index constraints against `max_locals` | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3, 4.9.1 | `Code` category-2 local variable instruction index constraints against `max_locals` | `jvm-classfile` | `CodeInstructionValidationTest` | IMPLEMENTED |
| 4.7.3 | `Code` `max_stack`, `max_locals`, `code_length`, and `code[]` header | `jvm-classfile` | `CodeAttributeHeaderParserTest` | IMPLEMENTED |
| 4.7.3 | `Code` exception table entries and `catch_type` class references | `jvm-classfile` | `CodeExceptionTableParserTest` | IMPLEMENTED |
| 4.7.3 | `Code` exception table `start_pc`, `end_pc`, and `handler_pc` instruction-boundary constraints | `jvm-classfile` | `CodeExceptionTableParserTest` | IMPLEMENTED |
| 4.7.3 | `Code` nested attributes table | `jvm-classfile` | `CodeNestedAttributesParserTest` | IMPLEMENTED |
| 4.7.3 | `Code` writer for header, exception table, and nested attributes | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.4 | `StackMapTable` classfile structure and all frame variants | `jvm-classfile` | `StackMapTableAttributeParserTest` | IMPLEMENTED |
| 4.7.4 | `StackMapTable` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.4 | `StackMapTable` frame expansion into verifier states | `jvm-verifier` | `StackMapFrameExpanderTest` | IMPLEMENTED |
| 4.7.4 | `StackMapTable` verifier semantics | `jvm-verifier` | TBD | PENDING |
| 4.7.5 | `Exceptions` | `jvm-classfile` | `ExceptionsAttributeParserTest` | IMPLEMENTED |
| 4.7.5 | `Exceptions` writer | `jvm-classfile` | `AttributeInfoWriterTest`, `MemberInfoWriterTest` | IMPLEMENTED |
| 4.7.6 | `InnerClasses` | `jvm-classfile` | `NestedClassAttributesParserTest` | IMPLEMENTED |
| 4.7.6 | `InnerClasses` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.7 | `EnclosingMethod` | `jvm-classfile` | `NestedClassAttributesParserTest` | IMPLEMENTED |
| 4.7.7 | `EnclosingMethod` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.8 | `Synthetic` | `jvm-classfile` | `SimpleAttributesParserTest` | IMPLEMENTED |
| 4.7.8 | `Synthetic` writer | `jvm-classfile` | `AttributeInfoWriterTest`, `MemberInfoWriterTest` | IMPLEMENTED |
| 4.7.9 | `Signature` attribute structure | `jvm-classfile` | `SignatureAttributeParserTest` | IMPLEMENTED |
| 4.7.9 | `Signature` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.9.1 | Field signature grammar | `jvm-classfile` | `SignatureAttributeParserTest` | IMPLEMENTED |
| 4.7.9.1 | Class and method signature grammar | `jvm-classfile` | `SignatureAttributeParserTest` | IMPLEMENTED |
| 4.7.10 | `SourceFile` | `jvm-classfile` | `SimpleAttributesParserTest` | IMPLEMENTED |
| 4.7.10 | `SourceFile` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.11 | `SourceDebugExtension` | `jvm-classfile` | `SourceDebugExtensionAttributeParserTest` | IMPLEMENTED |
| 4.7.11 | `SourceDebugExtension` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.12 | `LineNumberTable` | `jvm-classfile` | `LineNumberTableAttributeParserTest` | IMPLEMENTED |
| 4.7.12 | `LineNumberTable` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.13 | `LocalVariableTable` | `jvm-classfile` | `LocalVariableTableAttributeParserTest` | IMPLEMENTED |
| 4.7.13 | `LocalVariableTable` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.14 | `LocalVariableTypeTable` | `jvm-classfile` | `LocalVariableTypeTableAttributeParserTest` | IMPLEMENTED |
| 4.7.14 | `LocalVariableTypeTable` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.15 | `Deprecated` | `jvm-classfile` | `SimpleAttributesParserTest` | IMPLEMENTED |
| 4.7.15 | `Deprecated` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.16 | `RuntimeVisibleAnnotations` | `jvm-classfile` | `RuntimeVisibleAnnotationsAttributeParserTest` | IMPLEMENTED |
| 4.7.16 | `RuntimeVisibleAnnotations` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.17 | `RuntimeInvisibleAnnotations` | `jvm-classfile` | `RuntimeInvisibleAnnotationsAttributeParserTest` | IMPLEMENTED |
| 4.7.17 | `RuntimeInvisibleAnnotations` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.18 | `RuntimeVisibleParameterAnnotations` | `jvm-classfile` | `ParameterAnnotationsAttributeParserTest` | IMPLEMENTED |
| 4.7.18 | `RuntimeVisibleParameterAnnotations` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.19 | `RuntimeInvisibleParameterAnnotations` | `jvm-classfile` | `ParameterAnnotationsAttributeParserTest` | IMPLEMENTED |
| 4.7.19 | `RuntimeInvisibleParameterAnnotations` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.20 | `RuntimeVisibleTypeAnnotations` | `jvm-classfile` | `TypeAnnotationsAttributeParserTest` | IMPLEMENTED |
| 4.7.20 | `RuntimeVisibleTypeAnnotations` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.21 | `RuntimeInvisibleTypeAnnotations` | `jvm-classfile` | `TypeAnnotationsAttributeParserTest` | IMPLEMENTED |
| 4.7.21 | `RuntimeInvisibleTypeAnnotations` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.22 | `AnnotationDefault` | `jvm-classfile` | `AnnotationDefaultAttributeParserTest` | IMPLEMENTED |
| 4.7.22 | `AnnotationDefault` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.23 | `BootstrapMethods` classfile structure | `jvm-classfile`, `jvm-runtime` | `BootstrapMethodsAttributeParserTest` | IMPLEMENTED |
| 4.7.23 | `BootstrapMethods` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.23 | `BootstrapMethods` bootstrap argument resolution semantics | `jvm-runtime` | TBD | PENDING |
| 4.7.24 | `MethodParameters` classfile structure | `jvm-classfile` | `MethodParametersAttributeParserTest` | IMPLEMENTED |
| 4.7.24 | `MethodParameters` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.24 | `MethodParameters` formal parameter name grammar | `jvm-classfile` | `MethodParametersAttributeParserTest` | IMPLEMENTED |
| 4.7.25 | `Module` classfile structure | `jvm-classfile` | `ModuleAttributeParserTest` | IMPLEMENTED |
| 4.7.25 | `Module` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.25 | `Module` uniqueness and module relationship constraints | `jvm-classfile`, `jvm-runtime` | TBD | PENDING |
| 4.7.26 | `ModulePackages` classfile structure | `jvm-classfile` | `ModuleMetadataAttributesParserTest` | IMPLEMENTED |
| 4.7.26 | `ModulePackages` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.26 | `ModulePackages` uniqueness constraints | `jvm-classfile`, `jvm-runtime` | TBD | PENDING |
| 4.7.27 | `ModuleMainClass` | `jvm-classfile` | `ModuleMetadataAttributesParserTest` | IMPLEMENTED |
| 4.7.27 | `ModuleMainClass` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.28 | `NestHost` classfile structure | `jvm-classfile` | `NestAttributesParserTest` | IMPLEMENTED |
| 4.7.28 | `NestHost` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.28 | `NestHost` run-time package and access-control semantics | `jvm-runtime` | TBD | PENDING |
| 4.7.29 | `NestMembers` classfile structure | `jvm-classfile` | `NestAttributesParserTest` | IMPLEMENTED |
| 4.7.29 | `NestMembers` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.29 | `NestMembers` mutual-exclusion and access-control semantics | `jvm-classfile`, `jvm-runtime` | TBD | PENDING |
| 4.7.30 | `Record` classfile structure | `jvm-classfile` | `RecordAttributeParserTest` | IMPLEMENTED |
| 4.7.30 | `Record` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.30 | `Record` component name and descriptor grammar | `jvm-classfile` | `RecordAttributeParserTest` | IMPLEMENTED |
| 4.7.31 | `PermittedSubclasses` classfile structure | `jvm-classfile` | `PermittedSubclassesAttributeParserTest` | IMPLEMENTED |
| 4.7.31 | `PermittedSubclasses` writer | `jvm-classfile` | `AttributeInfoWriterTest` | IMPLEMENTED |
| 4.7.31 | `PermittedSubclasses` final-class and loading constraints | `jvm-classfile`, `jvm-runtime` | TBD | PENDING |
| 4.8 | Format checking | `jvm-classfile` | TBD | PENDING |
| 4.8 | Parser diagnostics carry source path and byte offset for attribute bodies | `jvm-classfile` | `AttributeParserRegistryTest`, `CodeNestedAttributesParserTest`, `ClassFileByteReaderTest` | IMPLEMENTED |
| 4.8 | Constant pool reference type and index checks | `jvm-classfile` | `ConstantPoolReferenceValidationTest` | IMPLEMENTED |
| 4.9 | Static and structural constraints | `jvm-verifier` | TBD | PENDING |
| 4.10.1 | Verification type model and StackMapTable type conversion | `jvm-verifier` | `VerificationTypeTest` | IMPLEMENTED |
| 4.10.1 | Verification type lattice base assignability | `jvm-verifier` | `VerificationTypeTest` | IMPLEMENTED |
| 4.10.1.2 | Class verification type `class(N,L)` lattice representation | `jvm-verifier` | `VerificationTypeLatticeTest` | IMPLEMENTED |
| 4.10.1.2 | Verification type lattice widens any class verification type to a loaded interface type | `jvm-verifier` | `VerificationTypeLatticeTest` | IMPLEMENTED |
| 4.10.1.2 | Verification type lattice widens class verification types to loaded superclass types | `jvm-verifier` | `VerificationTypeLatticeTest` | IMPLEMENTED |
| 4.10.1.2 | Verification type lattice widens same-name class types from different initiating loaders when they resolve to the same loaded class | `jvm-verifier` | `VerificationTypeLatticeTest` | IMPLEMENTED |
| 4.10.1.2 | Verification type lattice widens array types to bootstrap-defined `java/lang/Object` | `jvm-verifier` | `VerificationTypeLatticeTest` | IMPLEMENTED |
| 4.10.1.2 | Verification type lattice widens array types to bootstrap-defined `java/lang/Cloneable` | `jvm-verifier` | `VerificationTypeLatticeTest` | IMPLEMENTED |
| 4.10.1.2 | Verification type lattice widens array types to bootstrap-defined `java/io/Serializable` | `jvm-verifier` | `VerificationTypeLatticeTest` | IMPLEMENTED |
| 4.10.1.2 | Verification type lattice widens reference arrays covariantly through component widening | `jvm-verifier` | `VerificationTypeLatticeTest` | IMPLEMENTED |
| 4.10.1.5 | Method descriptor primitive parameter verification type parsing | `jvm-verifier` | `MethodDescriptorVerificationTypeParserTest` | IMPLEMENTED |
| 4.10.1.5 | Method descriptor class parameter verification type parsing | `jvm-verifier` | `MethodDescriptorVerificationTypeParserTest` | IMPLEMENTED |
| 4.10.1.5 | Method descriptor array parameter verification type parsing | `jvm-verifier` | `MethodDescriptorVerificationTypeParserTest` | IMPLEMENTED |
| 4.3.2, 4.10.1.5 | Standalone field descriptor verification type parsing | `jvm-verifier` | `MethodDescriptorVerificationTypeParserTest` | IMPLEMENTED |
| 4.10.1.5 | Method descriptor return verification type parsing | `jvm-verifier` | `MethodDescriptorVerificationTypeParserTest` | IMPLEMENTED |
| 4.10.1.5 | `expandTypeList` local slot expansion for two-word verification types | `jvm-verifier` | `VerificationTypeSlotExpanderTest` | IMPLEMENTED |
| 4.10.1.5 | Static method initial frame locals from descriptor arguments | `jvm-verifier` | `MethodInitialFrameBuilderTest` | IMPLEMENTED |
| 4.10.1.5 | Static method initial frame rejects descriptor locals exceeding `max_locals` | `jvm-verifier` | `MethodInitialFrameBuilderTest` | IMPLEMENTED |
| 4.10.1.5 | Instance method initial frame uses `class(N,L)` for non-constructor `this` | `jvm-verifier` | `MethodInitialFrameBuilderTest` | IMPLEMENTED |
| 4.10.1.5 | Subclass constructor initial frame uses `uninitializedThis` and `flagThisUninit` | `jvm-verifier` | `MethodInitialFrameBuilderTest` | IMPLEMENTED |
| 4.10.1.5 | Object constructor initial frame uses `class(java/lang/Object,L)` without `flagThisUninit` | `jvm-verifier` | `MethodInitialFrameBuilderTest` | IMPLEMENTED |
| 4.10.1 | Method control flow graph for fixed-size branches and exception handlers | `jvm-verifier` | `MethodControlFlowGraphTest` | IMPLEMENTED |
| 4.9 | Method control flow graph rejects execution falling off the end of code | `jvm-verifier` | `MethodControlFlowGraphTest` | IMPLEMENTED |
| 4.9, 6.5 | Method control flow graph for `tableswitch` jump table targets | `jvm-verifier` | `MethodControlFlowGraphTest` | IMPLEMENTED |
| 4.9, 6.5 | Method control flow graph for `lookupswitch` match-offset targets | `jvm-verifier` | `MethodControlFlowGraphTest` | IMPLEMENTED |
| 4.9, 6.5 | Method control flow graph rejects unsorted `lookupswitch` match values | `jvm-verifier` | `MethodControlFlowGraphTest` | IMPLEMENTED |
| 4.9, 6.5 | Method control flow graph decodes `wide` local-variable instruction forms | `jvm-verifier` | `MethodControlFlowGraphTest` | IMPLEMENTED |
| 6.5 | Method control flow graph treats `ret` and `wide ret` as non-fallthrough control transfers | `jvm-verifier` | `MethodControlFlowGraphTest` | IMPLEMENTED |
| 6.5 | Method control flow graph builds `jsr` and `jsr_w` subroutine branch targets | `jvm-verifier` | `MethodControlFlowGraphTest` | IMPLEMENTED |
| 4.10.1 | Verifier frame max locals and max stack resource limits | `jvm-verifier` | `MethodResourceLimitsVerifierTest` | IMPLEMENTED |
| 4.10.1 | Verifier local variable slot reads and writes | `jvm-verifier` | `VerifierLocalVariablesTest` | IMPLEMENTED |
| 4.10.1 | Verifier operand stack push and pop transfers | `jvm-verifier` | `VerifierOperandStackTest` | IMPLEMENTED |
| 4.10.1 | `uninitializedThis` constructor state transitions | `jvm-verifier` | `UninitializedThisRulesTest` | IMPLEMENTED |
| 4.10.1 | `new` object uninitialized state transitions | `jvm-verifier` | `ObjectInitializationRulesTest` | IMPLEMENTED |
| 4.10.1 | Exception handler edge frame derivation and target assignability | `jvm-verifier` | `ExceptionHandlerEdgesVerifierTest` | IMPLEMENTED |
| 4.10.1.8 | Protected member access receiver type checking | `jvm-verifier` | `ProtectedMemberAccessVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Constant push instruction operand stack transitions for null, int, long, float, and double values | `jvm-verifier` | `ConstantInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Local load instruction type transitions | `jvm-verifier` | `LocalLoadInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Local store instruction type transitions | `jvm-verifier` | `LocalStoreInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Iaload instruction int array operand stack transition | `jvm-verifier` | `IntArrayLoadInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Laload instruction long array operand stack transition | `jvm-verifier` | `LongArrayLoadInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Faload instruction float array operand stack transition | `jvm-verifier` | `FloatArrayLoadInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Daload instruction double array operand stack transition | `jvm-verifier` | `DoubleArrayLoadInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Aaload instruction reference array operand stack transition | `jvm-verifier` | `ReferenceArrayLoadInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Byte and boolean primitive array component verification atoms | `jvm-verifier` | `VerificationTypeTest` | IMPLEMENTED |
| 4.10.1 | Baload instruction byte boolean array operand stack transition | `jvm-verifier` | `ByteArrayLoadInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Char primitive array component verification atom | `jvm-verifier` | `VerificationTypeTest` | IMPLEMENTED |
| 4.10.1 | Caload instruction char array operand stack transition | `jvm-verifier` | `CharArrayLoadInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Short primitive array component verification atom | `jvm-verifier` | `VerificationTypeTest` | IMPLEMENTED |
| 4.10.1 | Saload instruction short array operand stack transition | `jvm-verifier` | `ShortArrayLoadInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Iastore instruction int array operand stack transition | `jvm-verifier` | `IntArrayStoreInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Lastore instruction long array operand stack transition | `jvm-verifier` | `LongArrayStoreInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Fastore instruction float array operand stack transition | `jvm-verifier` | `FloatArrayStoreInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Dastore instruction double array operand stack transition | `jvm-verifier` | `DoubleArrayStoreInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Aastore instruction reference array operand stack transition | `jvm-verifier` | `ReferenceArrayStoreInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Bastore instruction byte boolean array operand stack transition | `jvm-verifier` | `ByteArrayStoreInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Castore instruction char array operand stack transition | `jvm-verifier` | `CharArrayStoreInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Sastore instruction short array operand stack transition | `jvm-verifier` | `ShortArrayStoreInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int binary arithmetic instruction type transitions | `jvm-verifier` | `IntBinaryArithmeticInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Long binary arithmetic instruction type transitions | `jvm-verifier` | `LongBinaryArithmeticInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Float binary arithmetic instruction type transitions | `jvm-verifier` | `FloatBinaryArithmeticInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Double binary arithmetic instruction type transitions | `jvm-verifier` | `DoubleBinaryArithmeticInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int shift instruction type transitions | `jvm-verifier` | `IntShiftInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Long shift instruction type transitions | `jvm-verifier` | `LongShiftInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int negation instruction type transitions | `jvm-verifier` | `IntNegationInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Long negation instruction type transitions | `jvm-verifier` | `LongNegationInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Float negation instruction type transitions | `jvm-verifier` | `FloatNegationInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Double negation instruction type transitions | `jvm-verifier` | `DoubleNegationInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `iinc` instruction int local type check with unchanged type state | `jvm-verifier` | `IncrementInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `wide` local-load widened index equivalent type rule | `jvm-verifier` | `WideLocalLoadInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `wide` local-store widened index equivalent type rule | `jvm-verifier` | `WideLocalStoreInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `wide iinc` widened index equivalent type rule | `jvm-verifier` | `WideIncrementInstructionVerifierTest` | IMPLEMENTED |
| 2.3.3, 4.10.2.5, 6.5 | `ret` returnAddress local type check with unchanged type state | `jvm-verifier` | `RetInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `wide ret` widened index equivalent returnAddress local type rule | `jvm-verifier` | `WideRetInstructionVerifierTest` | IMPLEMENTED |
| 2.3.3, 4.10.2.5, 6.5 | `jsr` returnAddress operand-stack push transition | `jvm-verifier` | `JsrInstructionVerifierTest` | IMPLEMENTED |
| 2.3.3, 4.10.2.5, 6.5 | `jsr_w` returnAddress operand-stack push transition | `jvm-verifier` | `JsrWideInstructionVerifierTest` | IMPLEMENTED |
| 2.3.3, 6.5 | `astore` stores either reference or returnAddress stack values | `jvm-verifier` | `LocalStoreInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int-to-long conversion instruction type transition | `jvm-verifier` | `IntToLongConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int-to-float conversion instruction type transition | `jvm-verifier` | `IntToFloatConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int-to-double conversion instruction type transition | `jvm-verifier` | `IntToDoubleConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Long-to-int conversion instruction type transition | `jvm-verifier` | `LongToIntConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Long-to-float conversion instruction type transition | `jvm-verifier` | `LongToFloatConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Long-to-double conversion instruction type transition | `jvm-verifier` | `LongToDoubleConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Float-to-int conversion instruction type transition | `jvm-verifier` | `FloatToIntConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Float-to-long conversion instruction type transition | `jvm-verifier` | `FloatToLongConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Float-to-double conversion instruction type transition | `jvm-verifier` | `FloatToDoubleConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Double-to-int conversion instruction type transition | `jvm-verifier` | `DoubleToIntConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Double-to-long conversion instruction type transition | `jvm-verifier` | `DoubleToLongConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Double-to-float conversion instruction type transition | `jvm-verifier` | `DoubleToFloatConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int-to-byte conversion instruction type transition | `jvm-verifier` | `IntToByteConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int-to-char conversion instruction type transition | `jvm-verifier` | `IntToCharConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int-to-short conversion instruction type transition | `jvm-verifier` | `IntToShortConversionInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Long compare instruction type transition | `jvm-verifier` | `LongCompareInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Float compare less instruction type transition | `jvm-verifier` | `FloatCompareLessInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Float compare greater instruction type transition | `jvm-verifier` | `FloatCompareGreaterInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Double compare less instruction type transition | `jvm-verifier` | `DoubleCompareLessInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Double compare greater instruction type transition | `jvm-verifier` | `DoubleCompareGreaterInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int zero branch instruction operand stack transition | `jvm-verifier` | `IntZeroBranchInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int compare branch instruction operand stack transition | `jvm-verifier` | `IntCompareBranchInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Reference compare branch instruction operand stack transition | `jvm-verifier` | `ReferenceCompareBranchInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Reference null branch instruction operand stack transition | `jvm-verifier` | `ReferenceNullBranchInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Goto instruction no operand stack transition | `jvm-verifier` | `GotoInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `tableswitch` and `lookupswitch` int key operand stack transition | `jvm-verifier` | `SwitchInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `newarray` primitive array type code and int-count stack transition | `jvm-verifier` | `NewArrayInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `anewarray` class-or-array component and int-count stack transition | `jvm-verifier` | `ANewArrayInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `multianewarray` array dimensionality and int-count stack transition | `jvm-verifier` | `MultiANewArrayInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `getstatic` declared field type operand stack transition | `jvm-verifier` | `GetStaticInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `putstatic` declared field type operand stack transition | `jvm-verifier` | `PutStaticInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `getfield` receiver-to-declared-field operand stack transition | `jvm-verifier` | `GetFieldInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `putfield` receiver-and-value operand stack transition | `jvm-verifier` | `PutFieldInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1.8, 6.5 | `getfield` protected superclass member receiver narrowing | `jvm-verifier` | `GetFieldInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1.8, 6.5 | `putfield` protected superclass member receiver narrowing after value pop | `jvm-verifier` | `PutFieldInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `invokestatic` descriptor argument/return operand stack transition | `jvm-verifier` | `InvokeStaticInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `invokestatic` rejects `<init>` and `<clinit>` method targets | `jvm-verifier` | `InvokeStaticInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `invokevirtual` receiver plus descriptor argument/return operand stack transition | `jvm-verifier` | `InvokeVirtualInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `invokevirtual` rejects `<init>` and `<clinit>` method targets | `jvm-verifier` | `InvokeVirtualInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1.8, 6.5 | `invokevirtual` protected superclass method receiver narrowing after argument pop | `jvm-verifier` | `InvokeVirtualInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `invokespecial` non-`<init>` current receiver plus descriptor argument/return operand stack transition | `jvm-verifier` | `InvokeSpecialInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `invokespecial` non-`<init>` method owner is current class, superclass, or direct superinterface | `jvm-verifier` | `InvokeSpecialInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `invokespecial <init>` on `uninitializedThis` enforces void descriptor, current/direct-super owner, normal initialization, and exception-frame poisoning | `jvm-verifier` | `InvokeSpecialInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `invokespecial <init>` on `uninitialized(offset)` enforces void descriptor, matching `new` owner, normal initialization, and exception-frame poisoning | `jvm-verifier` | `InvokeSpecialInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1.8, 6.5 | `invokespecial <init>` on `uninitialized(offset)` applies protected constructor receiver narrowing to the initialized normal frame | `jvm-verifier` | `InvokeSpecialInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `invokeinterface` receiver plus descriptor argument/return operand stack transition and `count` validation | `jvm-verifier` | `InvokeInterfaceInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `invokeinterface` `count` validation counts category-2 descriptor parameters as two operand units | `jvm-verifier` | `InvokeInterfaceInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `invokedynamic` dynamic call site descriptor argument/return operand stack transition | `jvm-verifier` | `InvokeDynamicInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `nop` instruction no type-state transition | `jvm-verifier` | `NopInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `arraylength` array-or-null reference to int stack transition | `jvm-verifier` | `ArrayLengthInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `instanceof` class-or-array target and reference-to-int stack transition | `jvm-verifier` | `InstanceOfInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `checkcast` class-or-array target stack transition | `jvm-verifier` | `CheckCastInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | `monitorenter` and `monitorexit` reference operand stack transition | `jvm-verifier` | `MonitorInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Void return instruction type transition | `jvm-verifier` | `ReturnInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Int return instruction type transition | `jvm-verifier` | `IntReturnInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Long return instruction type transition | `jvm-verifier` | `LongReturnInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Float return instruction type transition | `jvm-verifier` | `FloatReturnInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Double return instruction type transition | `jvm-verifier` | `DoubleReturnInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Reference return instruction type transition | `jvm-verifier` | `ReferenceReturnInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Athrow instruction Throwable operand stack check | `jvm-verifier` | `AthrowInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Pop instruction category-1 operand stack transition | `jvm-verifier` | `PopInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Pop2 instruction category-1/category-2 operand stack transition | `jvm-verifier` | `Pop2InstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Dup instruction category-1 operand stack transition | `jvm-verifier` | `DupInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Dup_x1 instruction category-1 operand stack transition | `jvm-verifier` | `DupX1InstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Dup_x2 instruction category-1/category-2 operand stack transition | `jvm-verifier` | `DupX2InstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Dup2 instruction category-1/category-2 operand stack transition | `jvm-verifier` | `Dup2InstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Dup2_x1 instruction category-1/category-2 operand stack transition | `jvm-verifier` | `Dup2X1InstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Dup2_x2 instruction category-1/category-2 operand stack transition | `jvm-verifier` | `Dup2X2InstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Swap instruction category-1 operand stack transition | `jvm-verifier` | `SwapInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Type checking verifier entrypoint for resource limits and fixed-length CFG | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1.5 | Type checking StackMap/frame offsets merge with instruction stream | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1.5 | Type checking branch target StackMap frame requirement | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1.5 | Type checking exception handler target StackMap frame requirement | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1.5 | Type checking verifier treats initial frame as bytecode offset zero frame | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `aconst_null` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `iconst_m1` through `iconst_5` operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lconst_0` and `lconst_1` operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `fconst_0` through `fconst_2` operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dconst_0` and `dconst_1` operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `bipush` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `sipush` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Integer` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc_w` `CONSTANT_Integer` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc2_w` `CONSTANT_Long` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc2_w` `CONSTANT_Double` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc2_w` `CONSTANT_Dynamic` long descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc2_w` `CONSTANT_Dynamic` double descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Float` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_String` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Class` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_MethodHandle` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_MethodType` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Dynamic` int descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Dynamic` boolean descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Dynamic` byte descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Dynamic` char descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Dynamic` short descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Dynamic` float descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Dynamic` object descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Dynamic` int array descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Dynamic` object array descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldc` `CONSTANT_Dynamic` nested int array descriptor operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier rejects `ldc` `CONSTANT_Dynamic` long descriptor category mismatch at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier rejects `ldc` `CONSTANT_Dynamic` double descriptor category mismatch at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier rejects `ldc` `CONSTANT_Dynamic` void descriptor as an invalid field descriptor at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier rejects `ldc2_w` `CONSTANT_Dynamic` int descriptor category mismatch at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier rejects `ldc2_w` `CONSTANT_Dynamic` float descriptor category mismatch at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier rejects `ldc2_w` `CONSTANT_Dynamic` object descriptor category mismatch at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier rejects `ldc2_w` `CONSTANT_Dynamic` array descriptor category mismatch at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier rejects `ldc2_w` `CONSTANT_Dynamic` void descriptor as an invalid field descriptor at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ifeq` operand stack transition at an explicit source frame | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies all int-zero branch operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies int-compare branch operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies reference-compare branch operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies reference-null branch operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `tableswitch` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lookupswitch` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies int array load operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies long array load operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies float array load operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies double array load operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies reference array load operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies byte array load operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies char array load operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies short array load operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies int array store operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies long array store operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies float array store operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies double array store operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies reference array store operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies byte array store operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies char array store operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies short array store operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `getstatic` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `putstatic` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `getfield` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `putfield` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `invokevirtual` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `invokespecial` non-`<init>` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `invokespecial <init>` on `uninitialized(offset)` at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `invokestatic` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `invokeinterface` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `invokedynamic` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `new` uninitialized object transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `newarray` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `anewarray` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `multianewarray` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `arraylength` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `athrow` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `athrow` operand stack transition from the method initial frame | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `checkcast` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `instanceof` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `monitorenter` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `monitorexit` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `pop` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `pop2` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dup` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dup_x1` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dup_x2` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dup2` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dup2_x1` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dup2_x2` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `swap` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `iadd` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ladd` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `fadd` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dadd` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `isub` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lsub` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `fsub` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dsub` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `imul` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lmul` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `fmul` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dmul` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `idiv` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ldiv` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `fdiv` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ddiv` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `irem` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lrem` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `frem` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `drem` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ineg` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lneg` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `fneg` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dneg` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ishl` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lshl` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ishr` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lshr` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `iushr` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lushr` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `iand` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `land` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ior` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lor` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ixor` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lxor` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `iinc` local variable transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `wide iinc` local variable transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 2.3.3, 4.10.2.5, 6.5 | Type checking verifier applies `jsr` returnAddress operand-stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 2.3.3, 4.10.2.5, 6.5 | Type checking verifier applies `jsr_w` returnAddress operand-stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 2.3.3, 4.10.2.5, 6.5 | Type checking verifier applies `ret` returnAddress local variable transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 2.3.3, 4.10.2.5, 6.5 | Type checking verifier applies `wide ret` returnAddress local variable transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `return` declared return type transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ireturn` declared return type transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lreturn` declared return type transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `freturn` declared return type transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dreturn` declared return type transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `areturn` declared return type transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `i2l` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `i2f` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `i2d` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `l2i` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `l2f` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `l2d` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `f2i` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `f2l` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `f2d` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `d2i` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `d2l` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `d2f` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `i2b` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `i2c` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `i2s` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `lcmp` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `fcmpl` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `fcmpg` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dcmpl` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `dcmpg` operand stack transition at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies explicit int local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies explicit long local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies explicit float local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies explicit double local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies explicit reference local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies wide int local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies wide long local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies wide float local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies wide double local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies wide reference local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies wide int local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies wide long local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies wide float local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies wide double local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies wide reference local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies implicit int local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies implicit long local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies implicit float local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies implicit double local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies implicit reference local load transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies explicit int local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies explicit long local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies explicit float local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies explicit double local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies explicit reference local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies implicit int local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies implicit long local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies implicit float local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies implicit double local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies implicit reference local store transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1 | Verification by type checking | `jvm-verifier` | TBD | PENDING |
| 4.10.2 | Verification by type inference | `jvm-verifier` | TBD | PENDING |
| 4.11 | JVM limitations | `jvm-classfile`, `jvm-runtime` | TBD | PENDING |

## Chapter 5 - Loading, Linking, and Initializing

| JVMS | Requirement | Module | Tests | Status |
| --- | --- | --- | --- | --- |
| 5.1 | Run-time constant pool | `jvm-runtime` | TBD | PENDING |
| 5.1 | `CONSTANT_String` string constants reuse an interned guest `java/lang/String` for identical code points | `jvm-runtime`, `jvm-interpreter` | `JvmHeapTest`, `JvmInterpreterTest` | IMPLEMENTED |
| 5.2 | JVM startup | `jvm-runtime` | TBD | PENDING |
| 5.3.1 | Bootstrap class loader loading | `jvm-runtime` | TBD | PENDING |
| 5.3.2 | User-defined class loader loading | `jvm-runtime` | TBD | PENDING |
| 5.3.3 | Array class creation | `jvm-runtime` | TBD | PENDING |
| 5.3.4 | Loading constraints | `jvm-runtime` | TBD | PENDING |
| 5.3.5 | Deriving class from class file bytes | `jvm-runtime`, `jvm-classfile` | TBD | PENDING |
| 5.3.6 | Modules and layers | `jvm-runtime` | TBD | PENDING |
| 5.4.1 | Verification during linking | `jvm-verifier`, `jvm-runtime` | TBD | PENDING |
| 5.4.2 | Preparation | `jvm-runtime` | TBD | PENDING |
| 5.4.3.1 | Class and interface resolution | `jvm-runtime` | TBD | PENDING |
| 5.4.3.1, 5.4.3.2 | Field reference resolution throws guest `NoClassDefFoundError` when the referenced owner class cannot be resolved in strict class-resolution mode | `jvm-runtime`, `jvm-interpreter` | `JvmInterpreterTest.getstatic throws guest NoClassDefFoundError when field owner class is missing` | IMPLEMENTED |
| 5.4.3.2 | Field resolution | `jvm-runtime` | TBD | PENDING |
| 5.4.3.2 | Field resolution finds fields declared directly by the referenced class | `jvm-runtime` | `JvmFieldResolutionTest` | IMPLEMENTED |
| 5.4.3.2 | Field resolution searches direct superinterfaces before superclass lookup | `jvm-runtime` | `JvmFieldResolutionTest` | IMPLEMENTED |
| 5.4.3.2 | Field resolution recursively searches indirect superinterfaces | `jvm-runtime` | `JvmFieldResolutionTest` | IMPLEMENTED |
| 5.4.3.2 | Field resolution searches the superclass chain after superinterface lookup | `jvm-runtime` | `JvmFieldResolutionTest` | IMPLEMENTED |
| 5.4.3.2 | Field resolution throws guest `NoSuchFieldError` when lookup misses | `jvm-runtime` | `JvmFieldResolutionTest` | IMPLEMENTED |
| 5.4.3.3 | Method resolution | `jvm-runtime` | TBD | PENDING |
| 5.4.3.4 | Interface method resolution | `jvm-runtime` | TBD | PENDING |
| 5.4.3.5 | Method type and method handle resolution | `jvm-runtime` | TBD | PENDING |
| 5.4.3.6 | Dynamic constant and call site resolution | `jvm-runtime` | TBD | PENDING |
| 5.4.4 | Access control | `jvm-runtime` | TBD | PENDING |
| 5.4.5 | Method overriding | `jvm-runtime` | TBD | PENDING |
| 5.4.6 | Method selection | `jvm-runtime` | TBD | PENDING |
| 5.5 | Initialization | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 5.6 | Binding native method implementations | `jvm-native`, `jvm-jni`, `jvm-host` | TBD | PENDING |
| 5.7 | VM termination | `jvm-runtime` | TBD | PENDING |

## Chapters 6 and 7 - Instruction Set and Opcode Mnemonics

Each opcode requires decoder coverage, verifier coverage when applicable, interpreter behavior or specified reserved-opcode error, and at least one focused test.

| Group | Opcodes | Module | Tests | Status |
| --- | --- | --- | --- | --- |
| Opcode metadata | 0x00..0xff mnemonic, fixed-length, variable-length, and reserved opcode table | `jvm-interpreter` | `OpcodeMetadataTest` | IMPLEMENTED |
| Fixed-length decoder | Fixed-length non-reserved opcodes decode into offsets, metadata, and unsigned operands | `jvm-interpreter` | `BytecodeDecoderTest` | IMPLEMENTED |
| Tableswitch decoder | `tableswitch` 4-byte alignment, default/low/high header, and jump-offset table length | `jvm-interpreter` | `BytecodeDecoderTest` | IMPLEMENTED |
| Lookupswitch decoder | `lookupswitch` 4-byte alignment, default/npairs header, and match-offset pair table length | `jvm-interpreter` | `BytecodeDecoderTest` | IMPLEMENTED |
| Wide decoder | `wide` local-variable, `ret`, and `iinc` operand forms | `jvm-interpreter` | `BytecodeDecoderTest` | IMPLEMENTED |
| Constants | `nop`, `aconst_null`, `iconst_m1`, `iconst_0..5`, `lconst_0..1`, `fconst_0..2`, `dconst_0..1`, `bipush`, `sipush`, `ldc`, `ldc_w`, `ldc2_w` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Constants execution | `nop`, `aconst_null`, `iconst_m1..iconst_5`, `lconst_0..lconst_1`, `fconst_0..fconst_2`, `dconst_0..dconst_1`, `bipush`, and `sipush` mutate the runtime operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Constant pool execution | `ldc` resolves `CONSTANT_Integer` from the runtime constant pool and pushes the int value | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest` | IMPLEMENTED |
| Constant pool execution | `ldc` resolves `CONSTANT_Float` from the runtime constant pool and pushes the float value | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest` | IMPLEMENTED |
| Constant pool execution | `ldc` resolves `CONSTANT_String` through `CONSTANT_Utf8`, allocates a guest `java/lang/String`, and pushes the reference | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest` | IMPLEMENTED |
| Constant pool execution | `ldc` resolves `CONSTANT_Class` through `CONSTANT_Utf8`, interns a guest `java/lang/Class` mirror, and pushes the reference | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest` | IMPLEMENTED |
| Constant pool execution | `ldc` resolves `CONSTANT_MethodType` through `CONSTANT_Utf8`, interns a guest `java/lang/invoke/MethodType`, and pushes the reference | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest`, `JvmHeapTest` | IMPLEMENTED |
| Constant pool execution | `ldc` resolves `CONSTANT_MethodHandle`, validates `reference_kind` against the referenced constant kind, interns a guest `java/lang/invoke/MethodHandle`, and pushes the reference | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest`, `JvmHeapTest` | IMPLEMENTED |
| Constant pool execution | `ldc_w` resolves a two-byte `CONSTANT_Integer` runtime constant-pool index and pushes the int value | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest` | IMPLEMENTED |
| Constant pool execution | `ldc_w` resolves a two-byte `CONSTANT_String` runtime constant-pool index and pushes the guest `java/lang/String` reference | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest` | IMPLEMENTED |
| Constant pool execution | `ldc_w` resolves a two-byte `CONSTANT_Class` runtime constant-pool index and pushes the guest `java/lang/Class` mirror reference | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest` | IMPLEMENTED |
| Constant pool execution | `ldc_w` resolves a two-byte `CONSTANT_MethodType` runtime constant-pool index and pushes the guest `java/lang/invoke/MethodType` reference | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest`, `JvmHeapTest` | IMPLEMENTED |
| Constant pool execution | `ldc_w` resolves a two-byte `CONSTANT_MethodHandle` runtime constant-pool index and pushes the guest `java/lang/invoke/MethodHandle` reference | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest`, `JvmHeapTest` | IMPLEMENTED |
| Constant pool execution | `ldc2_w` resolves `CONSTANT_Long` from the runtime constant pool and pushes the long value | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest` | IMPLEMENTED |
| Constant pool execution | `ldc2_w` resolves `CONSTANT_Double` from the runtime constant pool and pushes the double value | `jvm-interpreter`, `jvm-runtime`, `jvm-classfile` | `JvmInterpreterTest` | IMPLEMENTED |
| Loads | `iload`, `lload`, `fload`, `dload`, `aload`, `_0.._3` forms | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Loads execution | `iload` and `iload_0..iload_3` load int values from the runtime local variable array onto the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Loads execution | `wide iload` loads int values from a two-byte local variable index onto the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Loads execution | `lload` and `lload_0..lload_3` load long values from the runtime local variable array onto the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Loads execution | `wide lload` loads long values from a two-byte local variable index onto the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Loads execution | `fload` and `fload_0..fload_3` load float values from the runtime local variable array onto the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Loads execution | `wide fload` loads float values from a two-byte local variable index onto the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Loads execution | `dload` and `dload_0..dload_3` load double values from the runtime local variable array onto the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Loads execution | `wide dload` loads double values from a two-byte local variable index onto the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Loads execution | `aload` and `aload_0..aload_3` load reference values, including `null`, from the runtime local variable array onto the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Loads execution | `wide aload` loads reference values from a two-byte local variable index onto the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Array loads | `iaload`, `laload`, `faload`, `daload`, `aaload`, `baload`, `caload`, `saload` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Stores | `istore`, `lstore`, `fstore`, `dstore`, `astore`, `_0.._3` forms | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Stores execution | `istore` and `istore_0..istore_3` pop int values from the operand stack into the runtime local variable array | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stores execution | `wide istore` pops int values into a runtime local variable selected by a two-byte local variable index | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stores execution | `lstore` and `lstore_0..lstore_3` pop long values from the operand stack into the runtime local variable array | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stores execution | `wide lstore` pops long values into a runtime local variable selected by a two-byte local variable index | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stores execution | `fstore` and `fstore_0..fstore_3` pop float values from the operand stack into the runtime local variable array | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stores execution | `wide fstore` pops float values into a runtime local variable selected by a two-byte local variable index | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stores execution | `dstore` and `dstore_0..dstore_3` pop double values from the operand stack into the runtime local variable array | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stores execution | `wide dstore` pops double values into a runtime local variable selected by a two-byte local variable index | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stores execution | `astore` and `astore_0..astore_3` pop reference values, including `null`, from the operand stack into the runtime local variable array | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stores execution | `wide astore` pops reference values into a runtime local variable selected by a two-byte local variable index | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Array stores | `iastore`, `lastore`, `fastore`, `dastore`, `aastore`, `bastore`, `castore`, `sastore` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Stack | `pop`, `pop2`, `dup`, `dup_x1`, `dup_x2`, `dup2`, `dup2_x1`, `dup2_x2`, `swap` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Stack execution | `pop` removes the top category-1 runtime operand stack value | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stack execution | `pop2` removes either the top category-2 runtime operand stack value or the top two category-1 runtime operand stack values | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stack execution | `dup` duplicates the top category-1 runtime operand stack value | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stack execution | `dup_x1` duplicates the top category-1 runtime operand stack value and inserts it two category-1 values down | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stack execution | `dup_x2` duplicates the top category-1 runtime operand stack value across all JVMS category-1/category-2 forms | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stack execution | `dup2` duplicates either the top category-2 runtime operand stack value or the top two category-1 runtime operand stack values | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stack execution | `dup2_x1` duplicates the top one or two runtime operand stack values and inserts them one value beneath across all JVMS category-1/category-2 forms | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stack execution | `dup2_x2` duplicates the top one or two runtime operand stack values and inserts them two, three, or four values down across all JVMS category-1/category-2 forms | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Stack execution | `swap` exchanges the top two category-1 runtime operand stack values | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer and long math | `iadd`, `ladd`, `isub`, `lsub`, `imul`, `lmul`, `idiv`, `ldiv`, `irem`, `lrem`, `ineg`, `lneg`, shifts, bitwise ops | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Integer math execution | `iadd` adds two int runtime operand stack values with 32-bit two's-complement overflow wrapping and no overflow exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer math execution | `isub` subtracts the top int runtime operand stack value from the next value with 32-bit two's-complement overflow wrapping and no overflow exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer math execution | `imul` multiplies two int runtime operand stack values with 32-bit two's-complement overflow wrapping and no overflow exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer math execution | `idiv` divides the next int runtime operand stack value by the top value with truncation toward zero, minimum-value overflow behavior, and `java/lang/ArithmeticException` signaling on zero divisor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer math execution | `irem` pushes the remainder of the next int runtime operand stack value divided by the top value with dividend-sign behavior, minimum-value overflow behavior, and `java/lang/ArithmeticException` signaling on zero divisor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer math execution | `ineg` negates the top int runtime operand stack value with 32-bit two's-complement overflow wrapping and no overflow exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer math execution | `ishl` shifts the next int runtime operand stack value left by the low five bits of the top int shift distance with 32-bit overflow wrapping and no overflow exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer math execution | `ishr` arithmetically shifts the next int runtime operand stack value right by the low five bits of the top int shift distance with sign extension and no runtime exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer math execution | `iushr` logically shifts the next int runtime operand stack value right by the low five bits of the top int shift distance with zero extension and no runtime exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer math execution | `iand` computes the bitwise conjunction of the top two int runtime operand stack values and pushes the int result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer math execution | `ior` computes the bitwise disjunction of the top two int runtime operand stack values and pushes the int result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Integer math execution | `ixor` computes the bitwise exclusive disjunction of the top two int runtime operand stack values and pushes the int result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `ladd` adds two long runtime operand stack values with 64-bit two's-complement overflow wrapping and no overflow exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `lsub` subtracts the top long runtime operand stack value from the next value with 64-bit two's-complement overflow wrapping and no overflow exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `lmul` multiplies two long runtime operand stack values with 64-bit two's-complement overflow wrapping and no overflow exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `ldiv` divides the next long runtime operand stack value by the top value with truncation toward zero, minimum-value overflow behavior, and `java/lang/ArithmeticException` signaling on zero divisor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `lrem` pushes the remainder of the next long runtime operand stack value divided by the top value with dividend-sign behavior, minimum-value overflow behavior, and `java/lang/ArithmeticException` signaling on zero divisor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `lneg` negates the top long runtime operand stack value with 64-bit two's-complement overflow wrapping and no overflow exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `lshl` shifts the next long runtime operand stack value left by the low six bits of the top int shift distance with 64-bit overflow wrapping and no overflow exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `lshr` arithmetically shifts the next long runtime operand stack value right by the low six bits of the top int shift distance with sign extension and no runtime exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `lushr` logically shifts the next long runtime operand stack value right by the low six bits of the top int shift distance with zero extension and no runtime exception | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `land` computes the bitwise conjunction of the top two long runtime operand stack values and pushes the long result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `lor` computes the bitwise disjunction of the top two long runtime operand stack values and pushes the long result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Long math execution | `lxor` computes the bitwise exclusive disjunction of the top two long runtime operand stack values and pushes the long result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Float and double math | `fadd`, `dadd`, `fsub`, `dsub`, `fmul`, `dmul`, `fdiv`, `ddiv`, `frem`, `drem`, `fneg`, `dneg` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Float math execution | `fadd` adds two float runtime operand stack values with IEEE 754 NaN, infinity, signed-zero, overflow, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Float math execution | `fsub` subtracts the top float runtime operand stack value from the next value with IEEE 754 NaN, infinity, signed-zero, overflow, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Float math execution | `fmul` multiplies two float runtime operand stack values with IEEE 754 NaN, infinity, signed-zero, overflow, underflow-to-zero, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Float math execution | `fdiv` divides the next float runtime operand stack value by the top value with IEEE 754 NaN, infinity, signed-zero, overflow, underflow-to-zero, division-by-zero, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Float math execution | `frem` pushes the C `fmod`-style remainder of the next float runtime operand stack value divided by the top value with NaN, infinity, zero-divisor, signed-zero, dividend-sign, truncating-division, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Float math execution | `fneg` negates the top float runtime operand stack value with NaN, infinity, signed-zero, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Double math execution | `dadd` adds two double runtime operand stack values with IEEE 754 NaN, infinity, signed-zero, overflow, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Double math execution | `dsub` subtracts the top double runtime operand stack value from the next value with IEEE 754 NaN, infinity, signed-zero, overflow, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Double math execution | `dmul` multiplies two double runtime operand stack values with IEEE 754 NaN, infinity, signed-zero, overflow, underflow-to-zero, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Double math execution | `ddiv` divides the next double runtime operand stack value by the top value with IEEE 754 NaN, infinity, signed-zero, overflow, underflow-to-zero, division-by-zero, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Double math execution | `drem` pushes the C `fmod`-style remainder of the next double runtime operand stack value divided by the top value with NaN, infinity, zero-divisor, signed-zero, dividend-sign, truncating-division, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Double math execution | `dneg` negates the top double runtime operand stack value with NaN, infinity, signed-zero, and no-runtime-exception behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Increment | `iinc` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Increment execution | `iinc` increments an int runtime local variable by a signed byte constant without touching the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Increment execution | `wide iinc` increments an int runtime local variable selected by a two-byte index by a signed short constant without touching the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversions | `i2l`, `i2f`, `i2d`, `l2i`, `l2f`, `l2d`, `f2i`, `f2l`, `f2d`, `d2i`, `d2l`, `d2f`, `i2b`, `i2c`, `i2s` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Conversion execution | `i2l` sign extends the top int runtime operand stack value to a long and pushes the category-two long result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `i2f` converts the top int runtime operand stack value to a float, including IEEE 754 rounding for values outside float precision | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `i2d` converts the top int runtime operand stack value to an exactly representable double and pushes the category-two double result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `l2i` truncates the top long runtime operand stack value to the low 32 bits and pushes the int result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `l2f` converts the top long runtime operand stack value to a float, including IEEE 754 rounding for values outside float precision | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `l2d` converts the top long runtime operand stack value to a double, including IEEE 754 rounding for values outside double precision | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `f2i` converts the top float runtime operand stack value to an int with NaN-to-zero, saturation, and truncation-toward-zero behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `f2l` converts the top float runtime operand stack value to a long with NaN-to-zero, saturation, and truncation-toward-zero behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `f2d` converts the top float runtime operand stack value to an exactly representable double while preserving NaN and infinity semantics | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `d2i` converts the top double runtime operand stack value to an int with NaN-to-zero, saturation, and truncation-toward-zero behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `d2l` converts the top double runtime operand stack value to a long with NaN-to-zero, saturation, and truncation-toward-zero behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `d2f` converts the top double runtime operand stack value to a float with IEEE 754 rounding, overflow, and NaN behavior | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `i2b` truncates the top int runtime operand stack value to 8 bits, sign-extends it, and pushes the int result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `i2c` truncates the top int runtime operand stack value to 16 bits, zero-extends it, and pushes the int result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Conversion execution | `i2s` truncates the top int runtime operand stack value to 16 bits, sign-extends it, and pushes the int result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Comparisons | `lcmp`, `fcmpl`, `fcmpg`, `dcmpl`, `dcmpg` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Comparison execution | `lcmp` compares the top two long runtime operand stack values and pushes `1`, `0`, or `-1` as an int result | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Comparison execution | `fcmpl` compares the top two float runtime operand stack values and pushes `-1` when either operand is NaN | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Comparison execution | `fcmpg` compares the top two float runtime operand stack values and pushes `1` when either operand is NaN | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Comparison execution | `dcmpl` compares the top two double runtime operand stack values and pushes `-1` when either operand is NaN | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Comparison execution | `dcmpg` compares the top two double runtime operand stack values and pushes `1` when either operand is NaN | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `ifeq` pops the top int runtime operand stack value, branches on zero using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `ifne` pops the top int runtime operand stack value, branches on non-zero using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `iflt` pops the top int runtime operand stack value, branches when it is less than zero using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `ifge` pops the top int runtime operand stack value, branches when it is greater than or equal to zero using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `ifgt` pops the top int runtime operand stack value, branches when it is greater than zero using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `ifle` pops the top int runtime operand stack value, branches when it is less than or equal to zero using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `if_icmpeq` pops the top two int runtime operand stack values, branches when they are equal using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `if_icmpne` pops the top two int runtime operand stack values, branches when they are not equal using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `if_icmplt` pops the top two int runtime operand stack values, branches when the first value is less than the second using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `if_icmpge` pops the top two int runtime operand stack values, branches when the first value is greater than or equal to the second using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `if_icmpgt` pops the top two int runtime operand stack values, branches when the first value is greater than the second using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `if_icmple` pops the top two int runtime operand stack values, branches when the first value is less than or equal to the second using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `if_acmpeq` pops the top two reference runtime operand stack values, branches when they are equal using VM reference identity, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `if_acmpne` pops the top two reference runtime operand stack values, branches when they are not equal using VM reference identity, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `ifnull` pops the top reference runtime operand stack value, branches when it is `null` using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `ifnonnull` pops the top reference runtime operand stack value, branches when it is not `null` using a signed 16-bit offset, and falls through otherwise | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `goto` branches unconditionally using a signed 16-bit offset | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `goto_w` branches unconditionally using a signed 32-bit offset | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Branches | `if_icmp*`, `if_acmp*`, `ifnull`, `ifnonnull`, `goto`, `goto_w` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Switches | `tableswitch` pops the top int key, accounts for 4-byte alignment padding, chooses a matching jump offset for `low..high`, and uses default otherwise | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Switches | `lookupswitch` pops the top int key, accounts for 4-byte alignment padding, scans sorted match-offset pairs, and uses default when no key matches | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Switches | `tableswitch`, `lookupswitch` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Legacy subroutines | `jsr` pushes the next instruction offset as a category-1 `returnAddress` value and branches using a signed 16-bit offset | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Legacy subroutines | `jsr_w` pushes the next instruction offset as a category-1 `returnAddress` value and branches using a signed 32-bit offset | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Legacy subroutines | `ret` loads a local `returnAddress` value and resumes execution at that instruction offset | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Legacy subroutines | `wide ret` loads a local `returnAddress` value using an unsigned 16-bit local variable index and resumes execution at that instruction offset | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Legacy subroutines | `jsr`, `jsr_w`, `ret`, `returnAddress` handling | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Returns | `ireturn`, `lreturn`, `freturn`, `dreturn`, `areturn`, `return` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Field access | `getstatic`, `putstatic`, `getfield`, `putfield` | `jvm-interpreter`, `jvm-runtime`, `jvm-verifier` | TBD | PENDING |
| Field access | Static fields are addressed by declaring owner, field name, and descriptor in runtime storage | `jvm-runtime` | `JvmStaticFieldsTest` | IMPLEMENTED |
| Field access | `getstatic` resolves a `CONSTANT_Fieldref` and pushes a prepared int static field value | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getstatic` uses field resolution to read the actual declaring superclass static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getstatic` throws guest `IncompatibleClassChangeError` when the resolved field is not static | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access, 5.4.4 | `getstatic` throws guest `IllegalAccessError` when the current class accesses another class private static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getstatic throws guest IllegalAccessError for private fields from another class` | IMPLEMENTED |
| Field access, 5.4.4 | `getstatic` throws guest `IllegalAccessError` when the current class accesses another package's package-private static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getstatic throws guest IllegalAccessError for package private fields from another package` | IMPLEMENTED |
| Field access, 5.4.4 | `getstatic` allows the current class to access the same package's package-private static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getstatic allows package private fields from the same package` | IMPLEMENTED |
| Field access, 5.4.4 | `getstatic` throws guest `IllegalAccessError` when the current class accesses another package's protected static field without being a subclass | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getstatic throws guest IllegalAccessError for protected fields from non subclass in another package` | IMPLEMENTED |
| Field access, 5.4.4 | `getstatic` allows the current class to access the same package's protected static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getstatic allows protected fields from the same package` | IMPLEMENTED |
| Field access, 5.4.4 | `getstatic` allows a subclass to access another package's protected superclass static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getstatic allows protected superclass fields from subclasses in another package` | IMPLEMENTED |
| Field access | `getstatic` pushes a prepared category-2 long static field value and accounts for two operand-stack slots | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getstatic` reads an unwritten int static field as the JVM default zero value | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getstatic` reads an unwritten reference static field as the JVM default null value | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getstatic` rejects a prepared static field value that does not match its field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getstatic` pushes an object reference assignable to the declared object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getstatic` pushes an array reference assignable to the declared array field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getstatic` pushes an array reference assignable to `java/lang/Object` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getstatic` pushes an array reference assignable to `java/lang/Cloneable` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getstatic` pushes an array reference assignable to `java/io/Serializable` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getstatic` rejects an object reference field value that is not assignable to the declared object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putstatic` resolves a `CONSTANT_Fieldref`, pops an int value, and stores it in static fields | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putstatic` uses field resolution to write the actual declaring superclass static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putstatic` throws guest `IncompatibleClassChangeError` when the resolved field is not static | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access, 5.4.4 | `putstatic` throws guest `IllegalAccessError` when the current class writes another class private static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putstatic throws guest IllegalAccessError for private fields from another class` | IMPLEMENTED |
| Field access, 5.4.4 | `putstatic` throws guest `IllegalAccessError` when the current class writes another package's package-private static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putstatic throws guest IllegalAccessError for package private fields from another package` | IMPLEMENTED |
| Field access, 5.4.4 | `putstatic` allows the current class to write the same package's package-private static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putstatic allows package private fields from the same package` | IMPLEMENTED |
| Field access, 5.4.4 | `putstatic` throws guest `IllegalAccessError` when the current class writes another package's protected static field without being a subclass | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putstatic throws guest IllegalAccessError for protected fields from non subclass in another package` | IMPLEMENTED |
| Field access, 5.4.4 | `putstatic` allows the current class to write the same package's protected static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putstatic allows protected fields from the same package` | IMPLEMENTED |
| Field access, 5.4.4 | `putstatic` allows a subclass to write another package's protected superclass static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putstatic allows protected superclass fields from subclasses in another package` | IMPLEMENTED |
| Field access | `putstatic` pops a category-2 long value, accounts for two operand-stack slots, and stores it in static fields | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putstatic` rejects a value that does not match its field descriptor before storing it | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putstatic` stores null into a reference static field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putstatic` stores an object reference assignable to the declared object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putstatic` stores an array reference assignable to the declared array field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putstatic` stores an array reference assignable to `java/lang/Object` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putstatic` stores an array reference assignable to `java/lang/Cloneable` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putstatic` stores an array reference assignable to `java/io/Serializable` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putstatic` rejects an object reference value that is not assignable to the declared object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | Heap stores instance field values per object reference and symbolic field identity | `jvm-runtime` | `JvmHeapTest` | IMPLEMENTED |
| Runtime heap | Heap shallow-clones objects by allocating a new guest identity, copying heap payload, and copying instance fields | `jvm-runtime` | `JvmHeapTest.heap shallow clones*` | IMPLEMENTED |
| Field access | `getfield` resolves a `CONSTANT_Fieldref`, pops a non-null object reference, and pushes an int instance field value | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getfield` uses field resolution to read the actual declaring superclass instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getfield` throws guest `IncompatibleClassChangeError` when the resolved field is static | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access, 5.4.4 | `getfield` throws guest `IllegalAccessError` when the current class reads another class private instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getfield throws guest IllegalAccessError for private fields from another class` | IMPLEMENTED |
| Field access, 5.4.4 | `getfield` throws guest `IllegalAccessError` when the current class reads another package's package-private instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getfield throws guest IllegalAccessError for package private fields from another package` | IMPLEMENTED |
| Field access, 5.4.4 | `getfield` allows the current class to read the same package's package-private instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getfield allows package private fields from the same package` | IMPLEMENTED |
| Field access, 5.4.4 | `getfield` throws guest `IllegalAccessError` when the current class reads another package's protected instance field without being a subclass | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getfield throws guest IllegalAccessError for protected fields from non subclass in another package` | IMPLEMENTED |
| Field access, 5.4.4 | `getfield` allows the current class to read the same package's protected instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getfield allows protected fields from the same package` | IMPLEMENTED |
| Field access, 5.4.4 | `getfield` throws guest `IllegalAccessError` when a subclass reads another package's protected superclass field on a receiver not assignable to the current class | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getfield throws guest IllegalAccessError for protected superclass fields on unrelated receivers` | IMPLEMENTED |
| Field access, 5.4.4 | `getfield` allows a subclass to read another package's protected superclass field on a receiver assignable to the current class | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.getfield allows protected superclass fields on subclass receivers from another package` | IMPLEMENTED |
| Field access | `getfield` pushes a category-2 long instance field value and accounts for two operand-stack slots | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getfield` reads an unwritten int instance field as the JVM default zero value | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getfield` reads an unwritten reference instance field as the JVM default null value | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getfield` pushes an object reference field value assignable to the declared object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getfield` pushes an array reference field value assignable to the declared array field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getfield` pushes an array reference field value assignable to `java/lang/Object` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getfield` pushes an array reference field value assignable to `java/lang/Cloneable` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getfield` pushes an array reference field value assignable to `java/io/Serializable` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getfield` throws a guest `NullPointerException` when objectref is null | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `getfield` rejects an object reference field value that is not assignable to the declared object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putfield` resolves a `CONSTANT_Fieldref`, pops an int value and non-null object reference, and stores the instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putfield` uses field resolution to write the actual declaring superclass instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putfield` throws guest `IncompatibleClassChangeError` when the resolved field is static | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access, 5.4.4 | `putfield` throws guest `IllegalAccessError` when the current class writes another class private instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putfield throws guest IllegalAccessError for private fields from another class` | IMPLEMENTED |
| Field access, 5.4.4 | `putfield` throws guest `IllegalAccessError` when the current class writes another package's package-private instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putfield throws guest IllegalAccessError for package private fields from another package` | IMPLEMENTED |
| Field access, 5.4.4 | `putfield` allows the current class to write the same package's package-private instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putfield allows package private fields from the same package` | IMPLEMENTED |
| Field access, 5.4.4 | `putfield` throws guest `IllegalAccessError` when the current class writes another package's protected instance field without being a subclass | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putfield throws guest IllegalAccessError for protected fields from non subclass in another package` | IMPLEMENTED |
| Field access, 5.4.4 | `putfield` allows the current class to write the same package's protected instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putfield allows protected fields from the same package` | IMPLEMENTED |
| Field access, 5.4.4 | `putfield` throws guest `IllegalAccessError` when a subclass writes another package's protected superclass field on a receiver not assignable to the current class | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putfield throws guest IllegalAccessError for protected superclass fields on unrelated receivers` | IMPLEMENTED |
| Field access, 5.4.4 | `putfield` allows a subclass to write another package's protected superclass field on a receiver assignable to the current class | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.putfield allows protected superclass fields on subclass receivers from another package` | IMPLEMENTED |
| Field access | `putfield` pops a category-2 long value plus object reference and stores it in the instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putfield` stores null into a reference instance field | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putfield` stores an object reference value assignable to the declared object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putfield` stores an array reference value assignable to the declared array field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putfield` stores an array reference value assignable to `java/lang/Object` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putfield` stores an array reference value assignable to `java/lang/Cloneable` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putfield` stores an array reference value assignable to `java/io/Serializable` declared as an object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putfield` throws a guest `NullPointerException` when objectref is null after popping the field value | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Field access | `putfield` rejects an object reference value that is not assignable to the declared object field descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Invocation | `invokevirtual`, `invokespecial`, `invokestatic`, `invokeinterface`, `invokedynamic` | `jvm-interpreter`, `jvm-runtime`, `jvm-verifier` | TBD | PENDING |
| Method resolution, 5.4.3.3 | Class method resolution finds a method declared directly by the referenced class and reports guest `NoSuchMethodError` on misses | `jvm-runtime` | `JvmMethodResolutionTest` | IMPLEMENTED |
| Method resolution, 5.4.3.3 | Class method resolution searches the superclass chain after the referenced class | `jvm-runtime` | `JvmMethodResolutionTest.method resolution searches the superclass chain after the referenced class` | IMPLEMENTED |
| Method resolution, 5.4.3.3, 2.9.1 | Class method resolution does not inherit instance initialization methods from superclasses | `jvm-runtime` | `JvmMethodResolutionTest.method resolution does not inherit instance initialization methods from superclasses` | IMPLEMENTED |
| Method resolution, 5.4.3.3, 5.4.6 | Virtual method lookup starts at the receiver class before searching its superclass chain | `jvm-runtime` | `JvmMethodResolutionTest.virtual method resolution starts at the receiver class before superclasses` | IMPLEMENTED |
| Runtime class hierarchy, 5.3, 5.4.3.3, 6.5 | Runtime class metadata exposes only the direct superclass name for constructor-owner checks | `jvm-runtime` | `JvmMethodResolutionTest.class hierarchy exposes only the direct superclass name` | IMPLEMENTED |
| Invocation, 6.5 | `invokestatic` resolves a no-argument static method, executes its code in a callee frame, and pushes an `int` return value to the caller operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic executes no argument int returning static method` | IMPLEMENTED |
| Invocation, 6.5 | `invokestatic` pops `int` arguments from the caller operand stack and stores them into callee local variables | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic passes int arguments into callee locals` | IMPLEMENTED |
| Invocation, 6.5 | `invokestatic` passes and returns category-2 `long` values while preserving two-slot operand/local accounting | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic passes and returns category two long values` | IMPLEMENTED |
| Invocation, 6.5 | `invokestatic` executes a `void` static method and leaves the caller operand stack unchanged on `return` | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic executes void static method without pushing a return value` | IMPLEMENTED |
| Invocation, 6.5 | `invokestatic` throws guest `IncompatibleClassChangeError` when the resolved method is not static | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic throws guest IncompatibleClassChangeError for instance methods` | IMPLEMENTED |
| Invocation, 6.5 | `invokestatic` rejects object return values not assignable to the declared reference return descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic rejects object return values not assignable to declared return class` | IMPLEMENTED |
| Invocation, 6.5 | `invokestatic` rejects object argument values not assignable to declared reference parameter descriptors | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic rejects object arguments not assignable to declared parameter class` | IMPLEMENTED |
| Invocation, 5.4.3.3, 6.5 | `invokestatic` propagates guest `NoSuchMethodError` when method resolution misses | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic throws guest NoSuchMethodError when method resolution misses` | IMPLEMENTED |
| Invocation, 5.4.3.3, 6.5 | `invokestatic` propagates guest `NoClassDefFoundError` when the method owner class cannot be resolved | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic throws guest NoClassDefFoundError when method owner class is missing` | IMPLEMENTED |
| Invocation, 6.5 | `invokestatic` throws guest `UnsatisfiedLinkError` when the resolved native static method has no linked native implementation | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic throws guest UnsatisfiedLinkError for unbound native methods` | IMPLEMENTED |
| Native methods, 2.5.5, 5.6, 6.5 | `invokestatic` dispatches a linked native static method to a Kotlin-layer intrinsic and pushes its descriptor-checked return value | `jvm-interpreter` | `JvmInterpreterTest.invokestatic executes bound native intrinsic methods` | IMPLEMENTED |
| Native methods, 2.5.5, 5.6, 6.5 | Native method resolution falls back to a simulated JNI binding when no Kotlin-layer intrinsic is registered for the resolved method | `jvm-interpreter` | `JvmInterpreterTest.invokestatic falls back to simulated JNI when no native intrinsic is bound` | IMPLEMENTED |
| Native methods, 2.5.5, 5.6, 6.5 | Native method resolution prefers Kotlin-layer intrinsics over simulated JNI bindings when both layers contain the same resolved method | `jvm-interpreter` | `JvmInterpreterTest.invokestatic prefers native intrinsics over simulated JNI bindings` | IMPLEMENTED |
| Native methods, 2.5.5, 5.6, 6.5 | Native intrinsic execution receives the native method owner as current class plus the active heap and static field state | `jvm-interpreter` | `JvmInterpreterTest.invokestatic executes native intrinsics with callee owner context` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.6 | `java/lang/Object.getClass:()Ljava/lang/Class;` returns the interned guest class mirror for the receiver runtime class | `jvm-interpreter`, `jvm-runtime` | `JvmVmIntrinsicsTest.Object getClass intrinsic*` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.6 | `java/lang/Object.hashCode:()I` returns a stable identity hash for the guest receiver object | `jvm-interpreter`, `jvm-runtime` | `JvmVmIntrinsicsTest.Object hashCode intrinsic*` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.6 | `java/lang/Object.clone:()Ljava/lang/Object;` shallow-clones guest arrays and Cloneable guest objects through heap guest-state copying | `jvm-interpreter`, `jvm-runtime` | `JvmVmIntrinsicsTest.Object clone intrinsic*` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.6 | `java/lang/Object.wait/notify/notifyAll` mutate guest monitor wait-set state through the native intrinsic context | `jvm-interpreter`, `jvm-runtime` | `JvmVmIntrinsicsTest.Object wait*`, `JvmVmIntrinsicsTest.Object notify*` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.6 | `java/lang/System.arraycopy:(Ljava/lang/Object;ILjava/lang/Object;II)V` copies primitive and reference guest arrays with overlap, range, type, and assignability checks | `jvm-interpreter`, `jvm-runtime` | `JvmVmIntrinsicsTest.System arraycopy*` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.6 | `java/lang/System.identityHashCode:(Ljava/lang/Object;)I` returns the guest object identity hash or zero for null | `jvm-interpreter`, `jvm-runtime` | `JvmVmIntrinsicsTest.System identityHashCode*` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.6 | `java/lang/System.currentTimeMillis:()J` and `java/lang/System.nanoTime:()J` return context-provided guest long clock values | `jvm-interpreter` | `JvmVmIntrinsicsTest.System currentTimeMillis*`, `JvmVmIntrinsicsTest.System nanoTime*` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.3, 5.4, 5.6 | `java/lang/Class.initClassName:()Ljava/lang/String;`, `isArray:()Z`, `isPrimitive:()Z`, `isInterface:()Z`, and `getSuperclass:()Ljava/lang/Class;` query guest class mirror payloads and hierarchy metadata | `jvm-interpreter`, `jvm-runtime` | `JvmVmIntrinsicsTest.Class *` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.6 | `java/lang/Throwable.fillInStackTrace:(I)Ljava/lang/Throwable;` records a context-provided guest stack trace payload on the receiver and returns that receiver | `jvm-interpreter`, `jvm-runtime` | `JvmVmIntrinsicsTest.Throwable fillInStackTrace*` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.5, 5.6 | `java/lang/String.intern:()Ljava/lang/String;` returns the canonical guest interned string reference for a string payload receiver | `jvm-interpreter`, `jvm-runtime` | `JvmVmIntrinsicsTest.String intern*` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.6 | `java/lang/Thread.currentThread:()Ljava/lang/Thread;` returns the canonical guest thread mirror for the native context current thread id | `jvm-interpreter`, `jvm-runtime` | `JvmVmIntrinsicsTest.Thread currentThread*` | IMPLEMENTED |
| Native intrinsics, 2.5.5, 5.6 | `java/lang/Thread.sleep:(J)V`, `sleep:(JI)V`, and `sleepNanos0:(J)V` validate guest sleep arguments and delegate simulated blocking to the native context sleep handler | `jvm-interpreter` | `JvmVmIntrinsicsTest.Thread sleep*` | IMPLEMENTED |
| Native intrinsics, Phase 15 behavior suite | VM intrinsic registry resolves the committed Phase 15 native intrinsic surface as a single behavior guard | `jvm-interpreter` | `JvmVmIntrinsicsTest.VM intrinsic registry*` | IMPLEMENTED |
| Native library loading, 2.5.5, 5.6 | Native library descriptors model logical library names, filesystem paths, JNI_OnLoad symbols, and exported guest method signatures with duplicate-export validation | `jvm-jni` | `JvmNativeLibraryDescriptorTest` | IMPLEMENTED |
| Native library loading, 5.6, JNI binding | Native symbol name resolver emits JNI short and long names with class, method, and parameter descriptor mangling | `jvm-jni` | `JvmNativeSymbolNameResolverTest` | IMPLEMENTED |
| Native library loading, 5.6, JNI binding | Panama downcall backend skeleton resolves descriptor exports and raw symbols through an injected symbol lookup and reports unresolved exports as link errors | `jvm-jni` | `JvmPanamaDowncallBackendTest` | IMPLEMENTED |
| Native library loading, 5.6, JNI binding | JNI_OnLoad binding resolves optional library initialization symbols into downcall targets without failing absent libraries | `jvm-jni` | `JvmPanamaDowncallBackendTest.Panama backend binds optional JNI_OnLoad symbols` | IMPLEMENTED |
| Native library loading, 5.6, JNI binding | Java_ native exports bind library descriptor export tables into guest-method keyed downcall targets | `jvm-jni` | `JvmPanamaDowncallBackendTest.Panama backend binds Java native exports by guest signature` | IMPLEMENTED |
| Native library loading, 5.6, JNI binding | Native export invocation frames pass the simulated JNIEnv as the first downcall argument before guest values | `jvm-jni` | `JvmPanamaDowncallBackendTest.Panama backend prepares native export invocations with simulated JNIEnv first` | IMPLEMENTED |
| Native library loading, 5.6, JNI binding | Native export invocation frames marshal primitive guest values into typed JNI primitive downcall arguments | `jvm-jni` | `JvmPanamaDowncallBackendTest.Panama backend marshals primitive guest values into JNI primitive arguments` | IMPLEMENTED |
| Native library loading, 5.6, JNI binding | Native export invocation frames marshal guest object references into simulated JNI local handles and null into a null handle | `jvm-jni` | `JvmPanamaDowncallBackendTest.Panama backend marshals object references into JNI handles` | IMPLEMENTED |
| Native library loading, 5.6, JNI binding | Native downcall returns marshal void to no guest value, primitive JNI values to guest primitive values, and object handles back to guest references or null | `jvm-jni` | `JvmPanamaDowncallBackendTest.Panama backend marshals JNI return values back into guest values` | IMPLEMENTED |
| Native library loading, 5.6, JNI binding | Native downcall returns propagate thrown guest throwable handles as VM boundary exceptions carrying the guest throwable reference | `jvm-jni` | `JvmPanamaDowncallBackendTest.Panama backend propagates native thrown guest exceptions` | IMPLEMENTED |
| Native library loading, 5.6, JNI binding | Tiny native library fixture compiles a DLL at test runtime, loads it through Panama FFM, and verifies a native-to-Kotlin upcall stub round trip | `jvm-jni` | `JvmPanamaDowncallBackendTest.Panama backend runs tiny native library with upcall fixture` | IMPLEMENTED |
| GUI shell, JavaFX | JavaFX application shell exposes a stable main application class, title metadata, and launch entry point with an initial scene | `jvm-gui` | `VisualizeJvmApplicationTest.application shell exposes JavaFX application metadata` | IMPLEMENTED |
| GUI shell, JavaFX | Project/classpath panel models ordered classpath entries and exposes a JavaFX side panel mounted in the root view | `jvm-gui` | `ProjectClasspathPanelTest` | IMPLEMENTED |
| GUI shell, JavaFX | Classpath import action accepts ordered `.jar` and `.class` files, rejects unsupported files, de-duplicates existing entries, and is exposed through the project panel API | `jvm-gui` | `ClasspathImportActionTest` | IMPLEMENTED |
| GUI shell, JavaFX | Class tree model discovers `.class` entries from imported class files and jars, de-duplicates by internal class name, and exposes a JavaFX TreeView mounted in the root view | `jvm-gui` | `ClassTreeViewTest` | IMPLEMENTED |
| GUI shell, JavaFX | Member list model extracts field and method names/descriptors from parsed class files and exposes a JavaFX ListView mounted in the root view | `jvm-gui` | `MemberListViewTest` | IMPLEMENTED |
| GUI shell, JavaFX | Bytecode instruction model decodes Code attribute bytes into offset/mnemonic/operand rows and exposes a JavaFX ListView mounted in the root view | `jvm-gui`, `jvm-interpreter` | `BytecodeInstructionViewTest` | IMPLEMENTED |
| GUI shell, JavaFX | Constant pool model lists all one-based constant-pool slots including unusable long/double placeholders and exposes a JavaFX ListView mounted in the root view | `jvm-gui`, `jvm-classfile` | `ConstantPoolViewTest` | IMPLEMENTED |
| GUI shell, JavaFX | Run configuration model derives classpath from the GUI project, normalizes main class names, preserves program arguments, and validates required startup fields | `jvm-gui` | `RunConfigurationModelTest` | IMPLEMENTED |
| GUI shell, JavaFX | Debugger control bar exposes deterministic Run/Step/Pause/Stop controls with initial enabled state and is mounted in the root view | `jvm-gui` | `DebuggerControlBarTest` | IMPLEMENTED |
| GUI shell, JavaFX | Debugger step controller routes Step control actions to the GUI engine boundary with the current run configuration while ignoring non-step actions | `jvm-gui` | `DebuggerStepControllerTest` | IMPLEMENTED |
| GUI runtime visualization | Current frame view formats the active class, method descriptor, and pc from an immutable frame snapshot and exposes an empty state when no frame is active | `jvm-gui` | `CurrentFrameViewTest` | IMPLEMENTED |
| GUI runtime visualization | Local variables view lists the active frame's local-variable slots from immutable frame snapshots and exposes an empty state when no frame is active | `jvm-gui` | `LocalVariablesViewTest` | IMPLEMENTED |
| GUI runtime visualization | Operand stack view lists the active frame's operand-stack values from immutable frame snapshots in bottom-to-top order and exposes an empty state when no frame is active | `jvm-gui` | `OperandStackViewTest` | IMPLEMENTED |
| GUI runtime visualization | Bytecode instruction model marks the instruction whose offset matches the active frame pc and clears the marker when no frame is active | `jvm-gui` | `CurrentInstructionHighlightTest` | IMPLEMENTED |
| GUI runtime visualization | Class loading events view formats immutable class-loading event snapshots with sequence, loader, class name, and source path | `jvm-gui` | `ClassLoadingEventsViewTest` | IMPLEMENTED |
| GUI runtime visualization | Linking events view formats immutable verification, preparation, and resolution event snapshots with sequence, class, phase, and target | `jvm-gui` | `LinkingEventsViewTest` | IMPLEMENTED |
| GUI runtime visualization | Initialization events view formats immutable class-initialization state transition snapshots with sequence, class, state, and trigger | `jvm-gui` | `InitializationEventsViewTest` | IMPLEMENTED |
| GUI runtime visualization | Verifier diagnostics view formats immutable verifier diagnostics with sequence, severity, method location, bytecode offset, and message | `jvm-gui` | `VerifierDiagnosticsViewTest` | IMPLEMENTED |
| GUI runtime visualization | Exception unwinding events view formats immutable throw, frame-unwind, handler-match, and uncaught snapshots with sequence, throwable, frame, and bytecode offset | `jvm-gui` | `ExceptionUnwindingEventsViewTest` | IMPLEMENTED |
| GUI runtime visualization | Monitor events view formats immutable enter, reenter, exit, and failure snapshots with sequence, object reference, thread, hold count, frame, and bytecode offset | `jvm-gui` | `MonitorEventsViewTest` | IMPLEMENTED |
| GUI runtime visualization | Invokedynamic and condy events view formats immutable bootstrap linkage, invocation, resolution, and cache-hit snapshots with sequence, constant-pool index, bootstrap method, name-and-type, descriptor, result, and bytecode offset | `jvm-gui` | `DynamicLinkageEventsViewTest` | IMPLEMENTED |
| GUI runtime visualization | Host delegation boundary view formats immutable host-call decisions, rejections, returns, and failures with sequence, policy, guest method identity, and boundary detail | `jvm-gui` | `HostDelegationEventsViewTest` | IMPLEMENTED |
| GUI runtime visualization | Native intrinsic frames view formats immutable Kotlin intrinsic enter, return, throw, and simulated-JNI fallback snapshots with sequence, nesting depth, intrinsic identity, guest method, and detail | `jvm-gui` | `NativeIntrinsicFramesViewTest` | IMPLEMENTED |
| GUI runtime visualization | Simulated JNI calls view formats immutable JNIEnv function enter, return, pending-exception, and failure snapshots with sequence, function name, local frame depth, arguments, result, and pending guest exception | `jvm-gui` | `SimulatedJniCallsViewTest` | IMPLEMENTED |
| GUI runtime visualization | JNI upcall nesting view formats immutable interpreter reentry, return-to-native, and guest-exception propagation snapshots with sequence, nesting depth, JNIEnv call, target method, receiver, arguments, and result | `jvm-gui` | `JniUpcallNestingViewTest` | IMPLEMENTED |
| GUI runtime visualization | JavaFX smoke test harness starts the toolkit once, runs assertions on the JavaFX application thread, and instantiates the root view with runtime visualization panes mounted | `jvm-gui` | `JavaFxSmokeTestHarnessTest` | IMPLEMENTED |
| Spec coverage gates, 6.5 | Opcode table coverage gate compares the runtime opcode metadata against an independent JVMS opcode table for all 256 byte values, including fixed-length, variable-length, named reserved, and generated reserved opcodes | `jvm-interpreter` | `OpcodeTableCoverageTest` | IMPLEMENTED |
| Spec coverage gates, 6.5 | Opcode execution coverage gate classifies every opcode table entry as implemented, method-return-only, not-yet-implemented, or reserved, with current unsupported JVMS opcodes named explicitly | `jvm-interpreter` | `OpcodeExecutionCoverageTest` | IMPLEMENTED |
| Spec coverage gates, 4.7 | Attribute parser coverage gate classifies every JVMS standard attribute name, records owner scopes, and binds every implemented parser to its focused parser test | `jvm-classfile` | `AttributeParserCoverageTest` | IMPLEMENTED |
| Spec coverage gates, 4.10 | Verifier rule coverage gate enumerates every verifier source file, assigns each to a JVMS verifier spec area, and requires a focused test or explicit internal-support classification | `jvm-verifier` | `VerifierRuleCoverageTest` | IMPLEMENTED |
| Spec coverage gates, 5.3 | Class loading coverage gate enumerates JVMS class/interface creation rules, records current unsupported loader obligations explicitly, and binds implemented runtime/GUI loading surfaces to focused tests | `jvm-runtime` | `ClassLoadingCoverageTest` | IMPLEMENTED |
| Spec coverage gates, 5.4.3 | Linking resolution coverage gate enumerates JVMS symbolic-reference resolution families, binds implemented field/method/interface-method paths to focused tests, and names current unresolved resolution obligations explicitly | `jvm-runtime` | `LinkingResolutionCoverageTest` | IMPLEMENTED |
| Spec coverage gates, 5.5 | Initialization coverage gate enumerates JVMS class initialization obligations, binds implemented static preparation/use paths to focused tests, and names current missing initialization state-machine work explicitly | `jvm-runtime` | `InitializationCoverageTest` | IMPLEMENTED |
| Spec coverage gates, 5.6 | Native resolver coverage gate enumerates the layered native binding path, enforces VM intrinsic lookup before simulated JNI fallback, records unresolved native errors, and names remaining native library lifecycle gaps explicitly | `jvm-interpreter`, `jvm-jni` | `NativeResolverCoverageTest` | IMPLEMENTED |
| Spec coverage gates, 5.6, JNI | Simulated JNI coverage gate enumerates guest-scoped JNIEnv helper families, binds implemented handle/class/member/field/string/array/monitor/upcall paths to focused tests, and names unsupported JNI helper groups explicitly | `jvm-jni`, `jvm-interpreter` | `SimulatedJniCoverageTest` | IMPLEMENTED |
| Spec example corpus, 3 | JVMS chapter 3 example corpus compiles representative Java sources for arithmetic/local variables, control flow/switch, invocation/construction, arrays, exceptions/finally, and synchronization into classfiles for oracle-driven follow-up tests | `jvm-asm-oracle` | `JvmsChapter3ExampleCorpusTest` | IMPLEMENTED |
| Spec negative corpus, 4 | Malformed classfile corpus records parser rejection fixtures for truncated headers, bad magic, unsupported future versions, zero constant_pool_count, and truncated UTF-8 constants with expected exception/message fragments | `jvm-classfile` | `MalformedClassfileCorpusTest` | IMPLEMENTED |
| Spec differential corpus, runtime | HotSpot runtime differential corpus compiles executable Java fixtures and invokes their static entrypoints on the host JVM to capture oracle results for arithmetic loops, string switch dispatch, arrays, exception handlers, and synchronized methods | `jvm-asm-oracle` | `HotSpotRuntimeDifferentialCorpusTest` | IMPLEMENTED |
| Simulated JNI, 2.5.5, 5.6, 6.5 | Simulated JNI bindings can upcall a resolved static guest method and re-enter interpreted execution with descriptor-checked arguments and returns | `jvm-interpreter` | `JvmInterpreterTest.simulated JNI bindings can upcall interpreted static guest methods` | IMPLEMENTED |
| Simulated JNI, 2.5.5, 5.6, 6.5 | Simulated JNI bindings can upcall a virtual instance guest method and re-enter interpreted execution with receiver local 0 plus descriptor-checked arguments and returns | `jvm-interpreter` | `JvmInterpreterTest.simulated JNI bindings can upcall interpreted instance guest methods` | IMPLEMENTED |
| Invocation, 6.5 | `invokestatic` resolves static interface methods from `CONSTANT_InterfaceMethodref` operands | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic resolves static interface methods from interface method references` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokestatic` throws guest `IllegalAccessError` when the current class invokes another class private static method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic throws guest IllegalAccessError for private methods from another class` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokestatic` allows the current class to invoke its own private static method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic allows private methods from the same class` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokestatic` throws guest `IllegalAccessError` when the current class invokes another package's package-private static method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic throws guest IllegalAccessError for package private methods from another package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokestatic` allows the current class to invoke the same package's package-private static method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic allows package private methods from the same package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokestatic` throws guest `IllegalAccessError` when the current class invokes another package's protected static method without being a subclass | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic throws guest IllegalAccessError for protected methods from non subclass in another package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokestatic` allows the current class to invoke the same package's protected static method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic allows protected methods from the same package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokestatic` allows a subclass to invoke another package's protected superclass static method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokestatic allows protected superclass methods from subclasses in another package` | IMPLEMENTED |
| Invocation, 6.5 | `invokespecial` resolves a no-argument instance method, moves `objectref` to callee local 0, executes its code in a callee frame, and pushes an `int` return value to the caller operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial executes no argument int returning instance method` | IMPLEMENTED |
| Invocation, 6.5 | `invokespecial` pops descriptor arguments from the caller operand stack and stores them into callee local variables after `objectref` local 0 | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial passes int arguments into callee locals after receiver` | IMPLEMENTED |
| Invocation, 6.5 | `invokespecial` executes a `void` instance method and leaves the caller operand stack unchanged after popping `objectref` | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial executes void instance method without pushing a return value` | IMPLEMENTED |
| Invocation, 6.5 | `invokespecial` throws guest `NullPointerException` when `objectref` is null | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial throws guest NullPointerException for null objectref` | IMPLEMENTED |
| Invocation, 6.5 | `invokespecial` throws guest `IncompatibleClassChangeError` when the resolved method is static | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial throws guest IncompatibleClassChangeError for static methods` | IMPLEMENTED |
| Invocation, 6.5 | `invokespecial` throws guest `UnsatisfiedLinkError` when the resolved native instance method has no linked native implementation | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial throws guest UnsatisfiedLinkError for unbound native methods` | IMPLEMENTED |
| Native methods, 2.5.5, 5.6, 6.5 | `invokespecial` dispatches a linked native instance method to a Kotlin-layer intrinsic with `objectref` and descriptor arguments | `jvm-interpreter` | `JvmInterpreterTest.invokespecial executes bound native intrinsic methods` | IMPLEMENTED |
| Invocation, 5.4.3.3, 6.5 | `invokespecial` propagates guest `NoSuchMethodError` when method resolution misses | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial throws guest NoSuchMethodError when method resolution misses` | IMPLEMENTED |
| Invocation, 5.4.3.3, 6.5 | `invokespecial` propagates guest `NoClassDefFoundError` when the method owner class cannot be resolved | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial throws guest NoClassDefFoundError when method owner class is missing` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokespecial` throws guest `IllegalAccessError` when the current class invokes another class private instance method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial throws guest IllegalAccessError for private methods from another class` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokespecial` allows the current class to invoke its own private instance method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial allows private methods from the same class` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokespecial` throws guest `IllegalAccessError` when the current class invokes another package's package-private instance method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial throws guest IllegalAccessError for package private methods from another package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokespecial` allows the current class to invoke the same package's package-private instance method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial allows package private methods from the same package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokespecial` throws guest `IllegalAccessError` when the current class invokes another package's protected instance method without being a subclass | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial throws guest IllegalAccessError for protected methods from non subclass in another package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokespecial` allows the current class to invoke the same package's protected instance method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial allows protected methods from the same package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokespecial` throws guest `IllegalAccessError` when a cross-package subclass invokes a protected superclass instance method on a receiver that is not the current class or its subclass | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial throws guest IllegalAccessError for protected superclass methods on non subclass receivers` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokespecial` allows a subclass to invoke another package's protected superclass instance method on a receiver of the current class | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial allows protected superclass methods from subclasses in another package` | IMPLEMENTED |
| Invocation, 6.5 | `invokespecial` rejects an object argument that is not assignable to the declared reference parameter descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial rejects object arguments that are not assignable to reference descriptors` | IMPLEMENTED |
| Invocation, 6.5 | `invokespecial` rejects an object return value that is not assignable to the declared reference return descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial rejects object returns that are not assignable to reference descriptors` | IMPLEMENTED |
| Invocation, 6.5 | `invokespecial` stores category-2 `long` arguments in two callee local variable slots and pushes category-2 `long` returns to the caller stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial passes and returns category two long values` | IMPLEMENTED |
| Invocation, 6.5 | `invokespecial` rejects an `objectref` whose runtime class is not assignable to the resolved method owner | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial rejects receivers that are not assignable to the resolved method owner` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` resolves a no-argument instance method, moves `objectref` to callee local 0, executes its code in a callee frame, and pushes an `int` return value | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual executes no argument int returning instance method` | IMPLEMENTED |
| Invocation, 5.4.6, 6.5 | `invokevirtual` selects the target method by starting dynamic lookup at the runtime receiver class | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual dispatches to receiver class override` | IMPLEMENTED |
| Invocation, 4.9.1, 6.5 | `invokevirtual` rejects instance initialization and class initialization method names before callee execution | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual rejects instance initialization method names` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` throws guest `IncompatibleClassChangeError` when the resolved method is static | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual throws guest IncompatibleClassChangeError for static methods` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` throws guest `IncompatibleClassChangeError` when virtual dispatch selects a static method in the receiver class | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual throws guest IncompatibleClassChangeError when selected method is static` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` throws guest `NullPointerException` when `objectref` is null | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual throws guest NullPointerException for null objectref` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` rejects an `objectref` whose runtime class is not assignable to the resolved method owner | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual rejects receivers that are not assignable to the resolved method owner` | IMPLEMENTED |
| Invocation, 5.4.3.3, 6.5 | `invokevirtual` propagates guest `NoClassDefFoundError` when the method owner class cannot be resolved | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual throws guest NoClassDefFoundError when method owner class is missing` | IMPLEMENTED |
| Invocation, 5.4.3.3, 6.5 | `invokevirtual` propagates guest `NoSuchMethodError` when method resolution misses | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual throws guest NoSuchMethodError when method resolution misses` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` throws guest `AbstractMethodError` when the selected target method is abstract | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual throws guest AbstractMethodError when selected method is abstract` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` throws guest `UnsatisfiedLinkError` when the selected native target method has no linked native implementation | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual throws guest UnsatisfiedLinkError for unbound native methods` | IMPLEMENTED |
| Native methods, 2.5.5, 5.6, 6.5 | `invokevirtual` dispatches a linked native instance method to a Kotlin-layer intrinsic with `objectref` and descriptor arguments | `jvm-interpreter` | `JvmInterpreterTest.invokevirtual executes bound native intrinsic methods` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` pops descriptor arguments from the caller operand stack and stores them into callee local variables after `objectref` local 0 | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual passes int arguments into callee locals after receiver` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` executes a `void` instance method and leaves the caller operand stack unchanged after popping `objectref` | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual executes void instance method without pushing a return value` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` stores category-2 `long` arguments in two callee local variable slots and pushes category-2 `long` returns to the caller stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual passes and returns category two long values` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` rejects an object argument that is not assignable to the declared reference parameter descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual rejects object arguments that are not assignable to reference descriptors` | IMPLEMENTED |
| Invocation, 6.5 | `invokevirtual` rejects an object return value that is not assignable to the declared reference return descriptor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual rejects object returns that are not assignable to reference descriptors` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokevirtual` throws guest `IllegalAccessError` when the current class invokes another class private instance method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual throws guest IllegalAccessError for private methods from another class` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokevirtual` allows the current class to invoke its own private instance method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual allows private methods from the same class` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokevirtual` throws guest `IllegalAccessError` when the current class invokes another package's package-private instance method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual throws guest IllegalAccessError for package private methods from another package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokevirtual` allows the current class to invoke the same package's package-private instance method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual allows package private methods from the same package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokevirtual` throws guest `IllegalAccessError` when the current class invokes another package's protected instance method without being a subclass | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual throws guest IllegalAccessError for protected methods from non subclass in another package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokevirtual` allows the current class to invoke the same package's protected instance method | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual allows protected methods from the same package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokevirtual` allows a subclass to invoke another package's protected superclass instance method on a receiver of the current class | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual allows protected superclass methods from subclasses in another package` | IMPLEMENTED |
| Method access, 5.4.4, 6.5 | `invokevirtual` throws guest `IllegalAccessError` when a cross-package subclass invokes a protected superclass instance method on a receiver that is not the current class or its subclass | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokevirtual throws guest IllegalAccessError for protected superclass methods on non subclass receivers` | IMPLEMENTED |
| Object creation, 2.4.2, 6.5 | Heap references can carry uninitialized object state until constructor execution marks them initialized | `jvm-runtime` | `JvmHeapTest.heap tracks uninitialized object state for constructor execution` | IMPLEMENTED |
| Object creation, 6.5 | `new` resolves a `CONSTANT_Class` name and allocates an uninitialized guest heap object reference for that class | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.new allocates an object for a CONSTANT_Class reference` | IMPLEMENTED |
| Object creation, 2.4.2, 6.5 | `invokespecial <init>` marks an uninitialized receiver initialized after the constructor returns normally | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial initializes an uninitialized object after constructor returns` | IMPLEMENTED |
| Invocation, 2.9.1, 6.5 | `invokespecial <init>` rejects constructor descriptors that do not return `void` | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial rejects constructor descriptors that do not return void` | IMPLEMENTED |
| Object creation, 2.4.2, 6.5 | `invokespecial <init>` rejects constructor calls on receivers that are already initialized | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial rejects constructor calls on initialized receivers` | IMPLEMENTED |
| Object creation, 2.4.2, 6.5 | Non-constructor `invokespecial` rejects uninitialized receivers before callee execution | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial rejects non constructor calls on uninitialized receivers` | IMPLEMENTED |
| Object creation, 2.9.1, 6.5 | `invokespecial <init>` allows a current constructor to initialize its receiver by invoking the direct superclass constructor | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial allows current constructor to initialize receiver through direct superclass constructor` | IMPLEMENTED |
| Object creation, 2.9.1, 6.5 | `invokespecial <init>` rejects superclass constructor calls outside the receiver class constructor context | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest.invokespecial rejects superclass constructor outside receiver constructor context` | IMPLEMENTED |
| Object and arrays | `anewarray` resolves an object `CONSTANT_Class`, pops an int count, and allocates a null-initialized guest reference array | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `arraylength` pops a guest `boolean[]` reference and pushes its length as an int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `arraylength` pops a guest `byte[]` reference and pushes its length as an int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `arraylength` pops a guest `char[]` reference and pushes its length as an int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `arraylength` pops a guest `double[]` reference and pushes its length as an int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `arraylength` pops a guest `float[]` reference and pushes its length as an int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `arraylength` pops a guest `short[]` reference and pushes its length as an int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `arraylength` pops a guest `int[]` reference and pushes its length as an int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `arraylength` pops a guest `long[]` reference and pushes its length as an int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `arraylength` pops a guest reference array reference and pushes its length as an int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `arraylength` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `iaload` pops an int index and guest `int[]` reference, then pushes the selected int element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `iaload` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `iaload` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `laload` pops an int index and guest `long[]` reference, then pushes the selected long element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `laload` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `laload` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `faload` pops an int index and guest `float[]` reference, then pushes the selected float element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `faload` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `faload` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `daload` pops an int index and guest `double[]` reference, then pushes the selected double element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `daload` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `daload` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `aaload` pops an int index and guest reference array, then pushes the selected reference element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `aaload` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `aaload` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `baload` pops an int index and guest `byte[]` reference, then pushes the selected byte element sign-extended to int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `baload` pops an int index and guest `boolean[]` reference, then pushes the selected boolean element as int `0` or `1` | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `baload` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `baload` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range `byte[]` index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `baload` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range `boolean[]` index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `caload` pops an int index and guest `char[]` reference, then pushes the selected char element zero-extended to int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `caload` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `caload` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `saload` pops an int index and guest `short[]` reference, then pushes the selected short element sign-extended to int | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `saload` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `saload` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `iastore` pops an int value, int index, and guest `int[]` reference, then stores the value into the selected element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `iastore` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `iastore` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `lastore` pops a long value, int index, and guest `long[]` reference, then stores the value into the selected element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `lastore` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `lastore` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `fastore` pops a float value, int index, and guest `float[]` reference, then stores the value into the selected element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `fastore` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `fastore` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `dastore` pops a double value, int index, and guest `double[]` reference, then stores the value into the selected element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `dastore` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `dastore` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `aastore` pops a reference value, int index, and guest reference-array reference, then stores the value into the selected element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `aastore` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `aastore` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `aastore` throws guest `ArrayStoreException` when an object element class differs from an exact reference-array component class | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `aastore` accepts an object element whose runtime class is assignable to the reference-array superclass component | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `aastore` accepts a covariant reference-array value whose component is assignable to the reference-array component | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `aastore` accepts an array object value for a `java/lang/Cloneable` reference-array component | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `aastore` accepts an array object value for a `java/io/Serializable` reference-array component | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `checkcast` resolves its target class and leaves a null object reference unchanged on the operand stack | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `checkcast` leaves an assignable non-null object reference unchanged on the operand stack | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `checkcast` throws guest `ClassCastException` when a non-null object reference is not assignable to the target type | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `instanceof` resolves its target class, pops a null object reference, and pushes int zero | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `instanceof` pops an assignable non-null object reference and pushes int one | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `instanceof` pops a non-null object reference that is not assignable to the target type and pushes int zero | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `bastore` pops an int value, int index, and guest `byte[]` reference, then stores the value narrowed to byte into the selected element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `bastore` pops an int value, int index, and guest `boolean[]` reference, then stores the value as a boolean into the selected element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `bastore` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `bastore` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range `byte[]` index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `bastore` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range `boolean[]` index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `castore` pops an int value, int index, and guest `char[]` reference, then stores the low unsigned 16 bits as a char into the selected element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `castore` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `castore` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `sastore` pops an int value, int index, and guest `short[]` reference, then stores the value narrowed to short into the selected element | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `sastore` throws guest `NullPointerException` for a null array reference | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `sastore` throws guest `ArrayIndexOutOfBoundsException` for an out-of-range int index | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `newarray` with `atype=T_BOOLEAN` pops an int count and allocates a zero-initialized guest `boolean[]` heap object | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `newarray` with `atype=T_BYTE` pops an int count and allocates a zero-initialized guest `byte[]` heap object | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `newarray` with `atype=T_CHAR` pops an int count and allocates a zero-initialized guest `char[]` heap object | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `newarray` with `atype=T_DOUBLE` pops an int count and allocates a zero-initialized guest `double[]` heap object | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `newarray` with `atype=T_FLOAT` pops an int count and allocates a zero-initialized guest `float[]` heap object | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `newarray` with `atype=T_INT` pops an int count and allocates a zero-initialized guest `int[]` heap object | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `newarray` with `atype=T_LONG` pops an int count and allocates a zero-initialized guest `long[]` heap object | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `newarray` throws guest `NegativeArraySizeException` when the popped count is negative | `jvm-interpreter` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `newarray` with `atype=T_SHORT` pops an int count and allocates a zero-initialized guest `short[]` heap object | `jvm-interpreter`, `jvm-runtime` | `JvmInterpreterTest` | IMPLEMENTED |
| Object and arrays | `new`, `newarray`, `anewarray`, `arraylength`, `multianewarray` | `jvm-interpreter`, `jvm-runtime`, `jvm-verifier` | TBD | PENDING |
| Type checks | `checkcast`, `instanceof` | `jvm-interpreter`, `jvm-runtime`, `jvm-verifier` | TBD | PENDING |
| Exceptions | `athrow` | `jvm-interpreter`, `jvm-runtime`, `jvm-verifier` | TBD | PENDING |
| Synchronization | `monitorenter`, `monitorexit` | `jvm-interpreter`, `jvm-runtime`, `jvm-verifier` | TBD | PENDING |
| Extended | `wide` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Reserved | `breakpoint`, `impdep1`, `impdep2` | `jvm-interpreter` | TBD | PENDING |

## Project-Specific Native and Host Coverage

| Area | Requirement | Module | Tests | Status |
| --- | --- | --- | --- | --- |
| Host delegation | JDK and configured whitelist classes may execute as opaque host boundaries | `jvm-host`, `jvm-runtime`, `jvm-gui` | TBD | PENDING |
| Native resolver | Interpreted class native calls resolve intrinsic first when whitelist allows | `jvm-native` | TBD | PENDING |
| Native resolver | Intrinsic miss falls back to simulated JNI | `jvm-native`, `jvm-jni` | TBD | PENDING |
| Simulated JNI | Custom `JNIEnv` function table routes upcalls to guest interpreter | `jvm-jni`, `jvm-interpreter` | TBD | PENDING |
| Simulated JNI | JNI handles represent guest objects, classes, methods, and fields | `jvm-jni`, `jvm-runtime` | `JvmJniHandleTableTest` | IMPLEMENTED |
| Simulated JNI | `FindClass` resolves loaded guest classes to `jclass` handles and throws guest `NoClassDefFoundError` for misses | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest` | IMPLEMENTED |
| Simulated JNI | `GetStaticMethodID` resolves static guest methods from a `jclass` handle into `jmethodID` handles and rejects misses or instance methods | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest` | IMPLEMENTED |
| Simulated JNI | `GetMethodID` resolves instance guest methods from a `jclass` handle into `jmethodID` handles and rejects misses or static methods | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest` | IMPLEMENTED |
| Simulated JNI | `GetObjectClass` resolves a guest `jobject` handle through the guest heap and returns the object's runtime class as a `jclass` handle | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetObjectClass returns runtime class handle for guest object handles` | IMPLEMENTED |
| Simulated JNI | `IsInstanceOf` tests a nullable guest `jobject` handle against a `jclass` handle using guest assignability rules | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.IsInstanceOf*` | IMPLEMENTED |
| Simulated JNI | `GetFieldID` resolves instance guest fields from a `jclass` handle into `jfieldID` handles and rejects misses or static fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetFieldID*` | IMPLEMENTED |
| Simulated JNI | `GetStaticFieldID` resolves static guest fields from a `jclass` handle into `jfieldID` handles and rejects misses or instance fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStaticFieldID*` | IMPLEMENTED |
| Simulated JNI | `GetIntField` reads guest instance `int` fields from `jobject` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetIntField*` | IMPLEMENTED |
| Simulated JNI | `SetIntField` writes guest instance `int` fields from `jobject` and `jfieldID` handles into guest heap state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetIntField*` | IMPLEMENTED |
| Simulated JNI | `GetLongField` reads guest instance `long` fields from `jobject` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetLongField*` | IMPLEMENTED |
| Simulated JNI | `SetLongField` writes guest instance `long` fields from `jobject` and `jfieldID` handles into guest heap state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetLongField*` | IMPLEMENTED |
| Simulated JNI | `GetFloatField` reads guest instance `float` fields from `jobject` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetFloatField*` | IMPLEMENTED |
| Simulated JNI | `SetFloatField` writes guest instance `float` fields from `jobject` and `jfieldID` handles into guest heap state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetFloatField*` | IMPLEMENTED |
| Simulated JNI | `GetDoubleField` reads guest instance `double` fields from `jobject` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetDoubleField*` | IMPLEMENTED |
| Simulated JNI | `SetDoubleField` writes guest instance `double` fields from `jobject` and `jfieldID` handles into guest heap state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetDoubleField*` | IMPLEMENTED |
| Simulated JNI | `GetBooleanField` reads guest instance `boolean` fields from `jobject` and `jfieldID` handles, including default false for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetBooleanField*` | IMPLEMENTED |
| Simulated JNI | `SetBooleanField` writes guest instance `boolean` fields from `jobject` and `jfieldID` handles into guest heap state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetBooleanField*` | IMPLEMENTED |
| Simulated JNI | `GetByteField` reads guest instance `byte` fields from `jobject` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetByteField*` | IMPLEMENTED |
| Simulated JNI | `SetByteField` writes guest instance `byte` fields from `jobject` and `jfieldID` handles into guest heap state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetByteField*` | IMPLEMENTED |
| Simulated JNI | `GetCharField` reads guest instance `char` fields from `jobject` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetCharField*` | IMPLEMENTED |
| Simulated JNI | `SetCharField` writes guest instance `char` fields from `jobject` and `jfieldID` handles into guest heap state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetCharField*` | IMPLEMENTED |
| Simulated JNI | `GetShortField` reads guest instance `short` fields from `jobject` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetShortField*` | IMPLEMENTED |
| Simulated JNI | `SetShortField` writes guest instance `short` fields from `jobject` and `jfieldID` handles into guest heap state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetShortField*` | IMPLEMENTED |
| Simulated JNI | `GetStaticIntField` reads guest static `int` fields from `jclass` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStaticIntField*` | IMPLEMENTED |
| Simulated JNI | `SetStaticIntField` writes guest static `int` fields from `jclass` and `jfieldID` handles into guest static field state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetStaticIntField*` | IMPLEMENTED |
| Simulated JNI | `GetStaticLongField` reads guest static `long` fields from `jclass` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStaticLongField*` | IMPLEMENTED |
| Simulated JNI | `SetStaticLongField` writes guest static `long` fields from `jclass` and `jfieldID` handles into guest static field state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetStaticLongField*` | IMPLEMENTED |
| Simulated JNI | `GetStaticFloatField` reads guest static `float` fields from `jclass` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStaticFloatField*` | IMPLEMENTED |
| Simulated JNI | `SetStaticFloatField` writes guest static `float` fields from `jclass` and `jfieldID` handles into guest static field state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetStaticFloatField*` | IMPLEMENTED |
| Simulated JNI | `GetStaticDoubleField` reads guest static `double` fields from `jclass` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStaticDoubleField*` | IMPLEMENTED |
| Simulated JNI | `SetStaticDoubleField` writes guest static `double` fields from `jclass` and `jfieldID` handles into guest static field state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetStaticDoubleField*` | IMPLEMENTED |
| Simulated JNI | `GetStaticBooleanField` reads guest static `boolean` fields from `jclass` and `jfieldID` handles, including default false for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStaticBooleanField*` | IMPLEMENTED |
| Simulated JNI | `SetStaticBooleanField` writes guest static `boolean` fields from `jclass` and `jfieldID` handles into guest static field state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetStaticBooleanField*` | IMPLEMENTED |
| Simulated JNI | `GetStaticObjectField` reads guest static reference fields from `jclass` and `jfieldID` handles, mapping guest null to null and object references to new JNI handles | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStaticObjectField*` | IMPLEMENTED |
| Simulated JNI | `SetStaticObjectField` writes guest static reference fields from `jclass` and `jfieldID` handles into guest static field state, including guest null | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetStaticObjectField*` | IMPLEMENTED |
| Simulated JNI | `GetStaticByteField` reads guest static `byte` fields from `jclass` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStaticByteField*` | IMPLEMENTED |
| Simulated JNI | `SetStaticByteField` writes guest static `byte` fields from `jclass` and `jfieldID` handles into guest static field state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetStaticByteField*` | IMPLEMENTED |
| Simulated JNI | `GetStaticCharField` reads guest static `char` fields from `jclass` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStaticCharField*` | IMPLEMENTED |
| Simulated JNI | `SetStaticCharField` writes guest static `char` fields from `jclass` and `jfieldID` handles into guest static field state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetStaticCharField*` | IMPLEMENTED |
| Simulated JNI | `GetStaticShortField` reads guest static `short` fields from `jclass` and `jfieldID` handles, including default zero for unwritten fields | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStaticShortField*` | IMPLEMENTED |
| Simulated JNI | `SetStaticShortField` writes guest static `short` fields from `jclass` and `jfieldID` handles into guest static field state | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetStaticShortField*` | IMPLEMENTED |
| Simulated JNI | `GetObjectField` reads guest instance reference fields from `jobject` and `jfieldID` handles, mapping guest null to null and object references to new JNI handles | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetObjectField*` | IMPLEMENTED |
| Simulated JNI | `SetObjectField` writes guest instance reference fields from `jobject` and `jfieldID` handles into guest heap state, including guest null | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetObjectField*` | IMPLEMENTED |
| Simulated JNI | `NewString` allocates a guest `java/lang/String` from UTF-16 code units and returns it as a local `jstring` handle | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.NewString*` | IMPLEMENTED |
| Simulated JNI | `NewStringUTF` allocates a guest `java/lang/String` from modified UTF input and returns it as a local `jstring` handle | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.NewStringUTF*` | IMPLEMENTED |
| Simulated JNI | `GetStringUTFLength` returns the modified UTF-8 byte length for guest `java/lang/String` handles and rejects non-string object handles | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStringUTFLength*` | IMPLEMENTED |
| Simulated JNI | `GetStringLength` returns the UTF-16 code unit length for guest `java/lang/String` handles and rejects non-string object handles | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStringLength*` | IMPLEMENTED |
| Simulated JNI | `GetStringChars` returns copied UTF-16 code units for guest `java/lang/String` handles and rejects non-string object handles | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStringChars*` | IMPLEMENTED |
| Simulated JNI | `GetStringUTFChars` returns copied modified UTF-8 bytes for guest `java/lang/String` handles and rejects non-string object handles | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetStringUTFChars*` | IMPLEMENTED |
| Simulated JNI | `ReleaseStringChars` validates guest `java/lang/String` handles and releases copied UTF-16 buffers as a simulated no-op | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.ReleaseStringChars*` | IMPLEMENTED |
| Simulated JNI | `ReleaseStringUTFChars` validates guest `java/lang/String` handles and releases copied modified UTF-8 buffers as a simulated no-op | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.ReleaseStringUTFChars*` | IMPLEMENTED |
| Simulated JNI | `GetArrayLength` returns primitive and reference guest array lengths and rejects non-array object handles | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetArrayLength*` | IMPLEMENTED |
| Simulated JNI | `NewBooleanArray` allocates false-filled guest boolean arrays as local `jbooleanArray` handles and rejects negative lengths | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.NewBooleanArray*` | IMPLEMENTED |
| Simulated JNI | `NewByteArray` allocates zero-filled guest byte arrays as local `jbyteArray` handles and rejects negative lengths | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.NewByteArray*` | IMPLEMENTED |
| Simulated JNI | `NewCharArray` allocates NUL-filled guest char arrays as local `jcharArray` handles and rejects negative lengths | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.NewCharArray*` | IMPLEMENTED |
| Simulated JNI | `NewShortArray` allocates zero-filled guest short arrays as local `jshortArray` handles and rejects negative lengths | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.NewShortArray*` | IMPLEMENTED |
| Simulated JNI | `NewIntArray` allocates zero-filled guest int arrays as local `jintArray` handles and rejects negative lengths | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.NewIntArray*` | IMPLEMENTED |
| Simulated JNI | `NewLongArray` allocates zero-filled guest long arrays as local `jlongArray` handles and rejects negative lengths | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.NewLongArray*` | IMPLEMENTED |
| Simulated JNI | `NewFloatArray` allocates positive-zero-filled guest float arrays as local `jfloatArray` handles and rejects negative lengths | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.NewFloatArray*` | IMPLEMENTED |
| Simulated JNI | `NewDoubleArray` allocates positive-zero-filled guest double arrays as local `jdoubleArray` handles and rejects negative lengths | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.NewDoubleArray*` | IMPLEMENTED |
| Simulated JNI | `GetBooleanArrayRegion` copies guest boolean array ranges into independent native buffers and rejects non-boolean arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetBooleanArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `GetBooleanArrayElements` returns a copied native boolean buffer for guest boolean arrays and rejects non-boolean arrays | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetBooleanArrayElements*` | IMPLEMENTED |
| Simulated JNI | `ReleaseBooleanArrayElements` applies default and commit copy-back modes to guest boolean arrays, supports abort without copy-back, and rejects invalid buffers | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.ReleaseBooleanArrayElements*` | IMPLEMENTED |
| Simulated JNI | `SetBooleanArrayRegion` copies native boolean buffers into guest boolean array ranges and rejects non-boolean arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetBooleanArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `GetByteArrayElements` returns a copied native byte buffer for guest byte arrays and rejects non-byte arrays | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetByteArrayElements*` | IMPLEMENTED |
| Simulated JNI | `ReleaseByteArrayElements` applies default and commit copy-back modes to guest byte arrays, supports abort without copy-back, and rejects invalid buffers | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.ReleaseByteArrayElements*` | IMPLEMENTED |
| Simulated JNI | `GetByteArrayRegion` copies guest byte array ranges into independent native buffers and rejects non-byte arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetByteArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `SetByteArrayRegion` copies native byte buffers into guest byte array ranges and rejects non-byte arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetByteArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `GetCharArrayElements` returns a copied native UTF-16 char buffer for guest char arrays and rejects non-char arrays | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetCharArrayElements*` | IMPLEMENTED |
| Simulated JNI | `ReleaseCharArrayElements` applies default and commit copy-back modes to guest char arrays, supports abort without copy-back, and rejects invalid buffers | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.ReleaseCharArrayElements*` | IMPLEMENTED |
| Simulated JNI | `GetCharArrayRegion` copies guest char array ranges into independent native buffers and rejects non-char arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetCharArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `SetCharArrayRegion` copies native char buffers into guest char array ranges and rejects non-char arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetCharArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `GetShortArrayElements` returns a copied native short buffer for guest short arrays and rejects non-short arrays | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetShortArrayElements*` | IMPLEMENTED |
| Simulated JNI | `ReleaseShortArrayElements` applies default and commit copy-back modes to guest short arrays, supports abort without copy-back, and rejects invalid buffers | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.ReleaseShortArrayElements*` | IMPLEMENTED |
| Simulated JNI | `getShortArrayRegion` copies guest short array ranges into independent native buffers and rejects non-short arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.getShortArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `setShortArrayRegion` copies native short buffers into guest short array ranges and rejects non-short arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.setShortArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `GetIntArrayElements` returns a copied native int buffer for guest int arrays and rejects non-int arrays | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetIntArrayElements*` | IMPLEMENTED |
| Simulated JNI | `ReleaseIntArrayElements` applies default and commit copy-back modes to guest int arrays, supports abort without copy-back, and rejects invalid buffers | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.ReleaseIntArrayElements*` | IMPLEMENTED |
| Simulated JNI | `GetIntArrayRegion` copies guest int array ranges into independent native buffers and rejects non-int arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetIntArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `SetIntArrayRegion` copies native int buffers into guest int array ranges and rejects non-int arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetIntArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `GetLongArrayElements` returns a copied native long buffer for guest long arrays and rejects non-long arrays | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetLongArrayElements*` | IMPLEMENTED |
| Simulated JNI | `ReleaseLongArrayElements` applies default and commit copy-back modes to guest long arrays, supports abort without copy-back, and rejects invalid buffers | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.ReleaseLongArrayElements*` | IMPLEMENTED |
| Simulated JNI | `GetLongArrayRegion` copies guest long array ranges into independent native buffers and rejects non-long arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetLongArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `SetLongArrayRegion` copies native long buffers into guest long array ranges and rejects non-long arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetLongArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `GetFloatArrayElements` returns a copied native float buffer for guest float arrays and rejects non-float arrays | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetFloatArrayElements*` | IMPLEMENTED |
| Simulated JNI | `ReleaseFloatArrayElements` applies default and commit copy-back modes to guest float arrays, supports abort without copy-back, and rejects invalid buffers | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.ReleaseFloatArrayElements*` | IMPLEMENTED |
| Simulated JNI | `GetFloatArrayRegion` copies guest float array ranges into independent native buffers and rejects non-float arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetFloatArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `SetFloatArrayRegion` copies native float buffers into guest float array ranges and rejects non-float arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetFloatArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `GetDoubleArrayElements` returns a copied native double buffer for guest double arrays and rejects non-double arrays | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetDoubleArrayElements*` | IMPLEMENTED |
| Simulated JNI | `ReleaseDoubleArrayElements` applies default and commit copy-back modes to guest double arrays, supports abort without copy-back, and rejects invalid buffers | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.ReleaseDoubleArrayElements*` | IMPLEMENTED |
| Simulated JNI | `GetDoubleArrayRegion` copies guest double array ranges into independent native buffers and rejects non-double arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetDoubleArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `SetDoubleArrayRegion` copies native double buffers into guest double array ranges and rejects non-double arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetDoubleArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `NewObjectArray` allocates guest reference arrays with null or assignable initial elements | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.NewObjectArray*` | IMPLEMENTED |
| Simulated JNI | `GetObjectArrayElement` reads nullable guest reference array elements into local object handles and rejects primitive arrays or out-of-bounds indexes | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetObjectArrayElement*` | IMPLEMENTED |
| Simulated JNI | `SetObjectArrayElement` writes nullable guest reference array elements and rejects primitive arrays, out-of-bounds indexes, or non-assignable values | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetObjectArrayElement*` | IMPLEMENTED |
| Simulated JNI | `GetObjectArrayRegion` copies nullable guest reference array ranges into local object handles and rejects primitive arrays or invalid ranges | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.GetObjectArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `SetObjectArrayRegion` writes nullable local handles into guest reference array ranges and rejects primitive arrays, invalid ranges, or non-assignable values | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.SetObjectArrayRegion*` | IMPLEMENTED |
| Simulated JNI | `MonitorEnter` resolves guest object handles and records reentrant ownership in guest monitor state for the current simulated JNI thread | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.MonitorEnter*` | IMPLEMENTED |
| Simulated JNI | `MonitorExit` resolves guest object handles and decrements or releases ownership in guest monitor state for the current simulated JNI thread | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.MonitorExit*` | IMPLEMENTED |
| Simulated JNI | JNI refs, strings, arrays, fields, and monitors mutate one shared guest state across helper families | `jvm-jni`, `jvm-runtime` | `JvmSimulatedJniEnvironmentTest.JNI data helpers mutate one guest state for refs strings arrays fields and monitors` | IMPLEMENTED |
| Unbound native | Missing native binding throws guest `UnsatisfiedLinkError` | `jvm-native`, `jvm-runtime` | TBD | PENDING |

## Documentation and Audit Coverage

These rows are project-level coverage artifacts that cross-reference normative JVMS work with implementation modules. They do not replace the chapter rows above; they make the final implementation/audit surface explicit.

| Area | Requirement | Module / artifact | Tests / validation | Status |
| --- | --- | --- | --- | --- |
| Architecture | Module responsibilities and dependency boundaries for the full JVMS implementation are documented | `docs/module-architecture.md` | `gradlew build`, manual doc review | IMPLEMENTED |
| Public API | Engine-facing API surface for class loading, verification, execution, events, host, native, and JNI is documented | `docs/public-engine-api.md` | `gradlew build`, manual doc review | IMPLEMENTED |
| Events | Ordered event stream contract for GUI/debugger observation is documented | `docs/event-stream-contract.md` | `gradlew build`, manual doc review | IMPLEMENTED |
| Classfile | Parser/writer/attribute/classfile coverage design is documented | `docs/classfile-coverage.md` | `AttributeParserCoverageTest`, `MalformedClassfileCorpusTest`, `ClassFileJavapDifferentialTest` | IMPLEMENTED |
| Verifier | JVMS 4.10 verifier pipeline, models, gates, and known gaps are documented | `docs/verifier-design.md` | `VerifierRuleCoverageTest`, `gradlew build` | IMPLEMENTED |
| Interpreter | Bytecode execution pipeline, opcode gates, invocation/native interaction, and known gaps are documented | `docs/interpreter-design.md` | `OpcodeTableCoverageTest`, `OpcodeExecutionCoverageTest`, HotSpot differential corpus | IMPLEMENTED |
| Host delegation | Default-interpreted host delegation policy, opaque event boundary, and bridge constraints are documented | `docs/host-delegation-policy.md` | `HostDelegationEventsView` model tests as they are added, `gradlew build` | IMPLEMENTED |
| Native resolver | Intrinsic-then-simulated-JNI native resolver layering is documented | `docs/layered-native-resolver.md` | `NativeResolverCoverageTest`, `JvmVmIntrinsicsTest`, `JvmPanamaDowncallBackendTest` | IMPLEMENTED |
| Simulated JNI | Guest-scoped JNI environment, handle model, helper families, and upcall rule are documented | `docs/simulated-jni-architecture.md` | `SimulatedJniCoverageTest`, `JvmSimulatedJniEnvironmentTest` | IMPLEMENTED |
| GUI | Import, inspection, debugging, event, host/native/JNI visualization workflow is documented | `docs/gui-workflow.md` | `JavaFX smoke test harness` planned, `gradlew build` | IMPLEMENTED |
| Coverage gates | Parser, opcode, verifier, loading/linking/init, native resolver, simulated JNI, malformed, and differential corpus gates are tracked | `docs/spec-coverage.md` and module coverage tests | `gradlew build` | IMPLEMENTED |
| Unsupported paths | Unsupported normative paths have an explicit audit step before final completion | `docs/spec-coverage.md`, `docs/unsupported-normative-paths-audit.md` | `gradlew build` | IMPLEMENTED |
| Final validation | Full build and smoke suite prove the final committed plan state | `docs/final-validation.md` | `gradlew build`, `gradlew test --rerun-tasks` | IMPLEMENTED |

## Final Gate

The project is not complete until every non-`N/A` row is at least `IMPLEMENTED`, and every row with an external comparison surface is `DIFFERENTIAL`.
