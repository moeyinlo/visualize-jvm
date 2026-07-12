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
| 4.10.1, 6.5 | Type checking verifier rejects `ldc` `CONSTANT_Dynamic` long descriptor category mismatch at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies `ifeq` operand stack transition at an explicit source frame | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies all int-zero branch operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies int-compare branch operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies reference-compare branch operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
| 4.10.1, 6.5 | Type checking verifier applies reference-null branch operand stack transitions at explicit source frames | `jvm-verifier` | `MethodTypeCheckingVerifierTest` | IMPLEMENTED |
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
