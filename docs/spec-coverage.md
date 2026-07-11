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
| 2.3.1 | Integral types and values | `jvm-runtime` | TBD | PENDING |
| 2.3.2 | Floating-point types and values | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.3.3 | `returnAddress` type and values | `jvm-runtime`, `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| 2.3.4 | `boolean` type and values | `jvm-runtime` | TBD | PENDING |
| 2.4 | Reference types and values | `jvm-runtime` | TBD | PENDING |
| 2.5.1 | pc register | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.5.2 | JVM stacks | `jvm-runtime` | TBD | PENDING |
| 2.5.3 | Heap | `jvm-runtime` | TBD | PENDING |
| 2.5.4 | Method area | `jvm-runtime` | TBD | PENDING |
| 2.5.5 | Run-time constant pool | `jvm-runtime` | TBD | PENDING |
| 2.5.6 | Native method stacks | `jvm-native`, `jvm-jni` | TBD | PENDING |
| 2.6 | Frames | `jvm-runtime` | TBD | PENDING |
| 2.6.1 | Local variables | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.6.2 | Operand stacks | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.6.3 | Dynamic linking | `jvm-runtime` | TBD | PENDING |
| 2.6.4 | Normal method invocation completion | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.6.5 | Abrupt method invocation completion | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.7 | Representation of objects | `jvm-runtime` | TBD | PENDING |
| 2.8 | Floating-point arithmetic | `jvm-interpreter` | TBD | PENDING |
| 2.9.1 | Instance initialization methods | `jvm-runtime`, `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| 2.9.2 | Class initialization methods | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
| 2.9.3 | Signature polymorphic methods | `jvm-runtime`, `jvm-interpreter` | TBD | PENDING |
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
| 4.10.1 | Method control flow graph for fixed-size branches and exception handlers | `jvm-verifier` | `MethodControlFlowGraphTest` | IMPLEMENTED |
| 4.10.1 | Verifier frame max locals and max stack resource limits | `jvm-verifier` | `MethodResourceLimitsVerifierTest` | IMPLEMENTED |
| 4.10.1 | Verifier local variable slot reads and writes | `jvm-verifier` | `VerifierLocalVariablesTest` | IMPLEMENTED |
| 4.10.1 | Verifier operand stack push and pop transfers | `jvm-verifier` | `VerifierOperandStackTest` | IMPLEMENTED |
| 4.10.1 | `uninitializedThis` constructor state transitions | `jvm-verifier` | `UninitializedThisRulesTest` | IMPLEMENTED |
| 4.10.1 | `new` object uninitialized state transitions | `jvm-verifier` | `ObjectInitializationRulesTest` | IMPLEMENTED |
| 4.10.1 | Exception handler edge frame derivation and target assignability | `jvm-verifier` | `ExceptionHandlerEdgesVerifierTest` | IMPLEMENTED |
| 4.10.1.8 | Protected member access receiver type checking | `jvm-verifier` | `ProtectedMemberAccessVerifierTest` | IMPLEMENTED |
| 4.10.1 | Local load instruction type transitions | `jvm-verifier` | `LocalLoadInstructionVerifierTest` | IMPLEMENTED |
| 4.10.1 | Local store instruction type transitions | `jvm-verifier` | `LocalStoreInstructionVerifierTest` | IMPLEMENTED |
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
| 4.10.1 | Verification by type checking | `jvm-verifier` | TBD | PENDING |
| 4.10.2 | Verification by type inference | `jvm-verifier` | TBD | PENDING |
| 4.11 | JVM limitations | `jvm-classfile`, `jvm-runtime` | TBD | PENDING |

## Chapter 5 - Loading, Linking, and Initializing

| JVMS | Requirement | Module | Tests | Status |
| --- | --- | --- | --- | --- |
| 5.1 | Run-time constant pool | `jvm-runtime` | TBD | PENDING |
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
| 5.4.3.2 | Field resolution | `jvm-runtime` | TBD | PENDING |
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
| Constants | `nop`, `aconst_null`, `iconst_m1`, `iconst_0..5`, `lconst_0..1`, `fconst_0..2`, `dconst_0..1`, `bipush`, `sipush`, `ldc`, `ldc_w`, `ldc2_w` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Loads | `iload`, `lload`, `fload`, `dload`, `aload`, `_0.._3` forms | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Array loads | `iaload`, `laload`, `faload`, `daload`, `aaload`, `baload`, `caload`, `saload` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Stores | `istore`, `lstore`, `fstore`, `dstore`, `astore`, `_0.._3` forms | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Array stores | `iastore`, `lastore`, `fastore`, `dastore`, `aastore`, `bastore`, `castore`, `sastore` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Stack | `pop`, `pop2`, `dup`, `dup_x1`, `dup_x2`, `dup2`, `dup2_x1`, `dup2_x2`, `swap` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Integer and long math | `iadd`, `ladd`, `isub`, `lsub`, `imul`, `lmul`, `idiv`, `ldiv`, `irem`, `lrem`, `ineg`, `lneg`, shifts, bitwise ops | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Float and double math | `fadd`, `dadd`, `fsub`, `dsub`, `fmul`, `dmul`, `fdiv`, `ddiv`, `frem`, `drem`, `fneg`, `dneg` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Increment | `iinc` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Conversions | `i2l`, `i2f`, `i2d`, `l2i`, `l2f`, `l2d`, `f2i`, `f2l`, `f2d`, `d2i`, `d2l`, `d2f`, `i2b`, `i2c`, `i2s` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Comparisons | `lcmp`, `fcmpl`, `fcmpg`, `dcmpl`, `dcmpg` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Branches | `ifeq`, `ifne`, `iflt`, `ifge`, `ifgt`, `ifle`, `if_icmp*`, `if_acmp*`, `ifnull`, `ifnonnull`, `goto`, `goto_w` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Switches | `tableswitch`, `lookupswitch` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Legacy subroutines | `jsr`, `jsr_w`, `ret`, `returnAddress` handling | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Returns | `ireturn`, `lreturn`, `freturn`, `dreturn`, `areturn`, `return` | `jvm-interpreter`, `jvm-verifier` | TBD | PENDING |
| Field access | `getstatic`, `putstatic`, `getfield`, `putfield` | `jvm-interpreter`, `jvm-runtime`, `jvm-verifier` | TBD | PENDING |
| Invocation | `invokevirtual`, `invokespecial`, `invokestatic`, `invokeinterface`, `invokedynamic` | `jvm-interpreter`, `jvm-runtime`, `jvm-verifier` | TBD | PENDING |
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
| Simulated JNI | JNI handles represent guest objects, classes, methods, and fields | `jvm-jni`, `jvm-runtime` | TBD | PENDING |
| Simulated JNI | JNI refs, exceptions, strings, arrays, fields, and monitors mutate guest state | `jvm-jni`, `jvm-runtime` | TBD | PENDING |
| Unbound native | Missing native binding throws guest `UnsatisfiedLinkError` | `jvm-native`, `jvm-runtime` | TBD | PENDING |

## Final Gate

The project is not complete until every non-`N/A` row is at least `IMPLEMENTED`, and every row with an external comparison surface is `DIFFERENTIAL`.
