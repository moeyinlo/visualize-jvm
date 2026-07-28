package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClassFileParserTest {
    @Test
    fun `parses complete ClassFile structure`() {
        val classFile = ClassFileParser.parse(
            bytes = minimalClassFileBytes(),
            source = "Minimal.class",
            attributeParsers = AttributeParserRegistry.of("SourceFile" to SourceFileAttributeParser),
        )

        assertEquals(ClassFileHeaderParser.ExpectedMagic, classFile.magic.value)
        assertEquals(70, classFile.version.major)
        assertEquals(7, classFile.constantPool.constantPoolCount)
        assertEquals(0x0021, classFile.accessFlags.raw)
        assertEquals(ClassFileKind.Class, classFile.accessFlags.kind)
        assertEquals(ConstantPoolIndex(2), classFile.identity.thisClassIndex)
        assertEquals(ConstantPoolIndex(4), classFile.identity.superClassIndex)
        assertEquals(emptyList(), classFile.identity.interfaceIndexes)
        assertEquals(emptyList(), classFile.fields)
        assertEquals(emptyList(), classFile.methods)
        assertEquals(ConstantPoolIndex(6), assertIs<SourceFileAttribute>(classFile.attributes.single()).sourceFileIndex)
    }

    @Test
    fun `rejects trailing bytes after ClassFile`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = minimalClassFileBytes() + 0,
                source = "Trailing.class",
                attributeParsers = AttributeParserRegistry.of("SourceFile" to SourceFileAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("Trailing bytes"), failure.message)
        assertTrue(failure.message.orEmpty().contains("remaining=1"), failure.message)
    }

    @Test
    fun `rejects this class index that is not a class constant`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithNonClassThisClassBytes(),
                source = "NonClassThis.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("this_class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("#2"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Class_info"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ConstantUtf8Entry"), failure.message)
    }

    @Test
    fun `rejects this class array names`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithArrayThisClassBytes(),
                source = "ArrayThis.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("this_class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("[Ljava/lang/Object;"), failure.message)
        assertTrue(failure.message.orEmpty().contains("array"), failure.message)
    }

    @Test
    fun `rejects super class index that is not a class constant`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithNonClassSuperClassBytes(),
                source = "NonClassSuper.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("super_class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("#4"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Class_info"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ConstantUtf8Entry"), failure.message)
    }

    @Test
    fun `rejects super class array names`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithArraySuperClassBytes(),
                source = "ArraySuper.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("super_class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("[Ljava/lang/Object;"), failure.message)
        assertTrue(failure.message.orEmpty().contains("array"), failure.message)
    }

    @Test
    fun `rejects interface index that is not a class constant`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithNonClassInterfaceBytes(),
                source = "NonClassInterface.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("interfaces[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("#5"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Class_info"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ConstantUtf8Entry"), failure.message)
    }

    @Test
    fun `rejects interface array names`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithArrayInterfaceBytes(),
                source = "ArrayInterface.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("interfaces[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("[Ljava/lang/Object;"), failure.message)
        assertTrue(failure.message.orEmpty().contains("array"), failure.message)
    }

    @Test
    fun `rejects duplicate interface indexes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateInterfaceIndexesBytes(),
                source = "DuplicateInterfaces.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("interfaces[1]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("duplicate"), failure.message)
        assertTrue(failure.message.orEmpty().contains("interfaces[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("#4"), failure.message)
    }

    @Test
    fun `rejects duplicate interface names through different class indexes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateInterfaceNamesBytes(),
                source = "DuplicateInterfaceNames.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("interfaces[1]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("duplicate"), failure.message)
        assertTrue(failure.message.orEmpty().contains("interfaces[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("java/lang/Object"), failure.message)
    }

    @Test
    fun `rejects zero super class on non Object class`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithZeroSuperNonObjectBytes(),
                source = "ZeroSuperNonObject.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("super_class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
        assertTrue(failure.message.orEmpty().contains("java/lang/Object"), failure.message)
        assertTrue(failure.message.orEmpty().contains("Test"), failure.message)
    }

    @Test
    fun `rejects nonzero super class on Object class`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = objectClassFileWithNonzeroSuperBytes(),
                source = "ObjectWithSuper.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("super_class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("java/lang/Object"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
    }

    @Test
    fun `rejects interface whose super class is not Object`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = interfaceClassFileWithNonObjectSuperBytes(),
                source = "InterfaceWithNonObjectSuper.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("super_class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("interface"), failure.message)
        assertTrue(failure.message.orEmpty().contains("java/lang/Object"), failure.message)
        assertTrue(failure.message.orEmpty().contains("other/Super"), failure.message)
    }

    @Test
    fun `rejects module classfile whose this class is not module info`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = moduleClassFileWithNonModuleInfoThisClassBytes(),
                source = "BadModuleName.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_MODULE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("this_class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("module-info"), failure.message)
        assertTrue(failure.message.orEmpty().contains("Test"), failure.message)
    }

    @Test
    fun `rejects module classfile with nonzero super class`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = moduleClassFileWithNonzeroSuperBytes(),
                source = "ModuleWithSuper.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_MODULE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("super_class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
    }

    @Test
    fun `rejects module classfile with interfaces`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = moduleClassFileWithInterfaceBytes(),
                source = "ModuleWithInterface.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_MODULE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("interfaces"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
    }

    @Test
    fun `rejects module classfile with fields`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = moduleClassFileWithFieldBytes(),
                source = "ModuleWithField.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_MODULE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("fields_count"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
    }

    @Test
    fun `rejects module classfile with methods`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = moduleClassFileWithMethodBytes(),
                source = "ModuleWithMethod.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_MODULE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("methods_count"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
    }

    @Test
    fun `rejects module classfile without Module attribute`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = moduleClassFileWithoutModuleAttributeBytes(),
                source = "ModuleWithoutModuleAttribute.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_MODULE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("Module"), failure.message)
        assertTrue(failure.message.orEmpty().contains("exactly one"), failure.message)
    }

    @Test
    fun `rejects module classfile with disallowed predefined attribute`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = moduleClassFileWithBootstrapMethodsBytes(),
                source = "ModuleWithBootstrapMethods.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_MODULE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("BootstrapMethods"), failure.message)
        assertTrue(failure.message.orEmpty().contains("predefined"), failure.message)
    }

    @Test
    fun `rejects module constants in ordinary classfiles`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithModuleConstantBytes(),
                source = "OrdinaryWithModuleConstant.class",
                attributeParsers = AttributeParserRegistry.of("SourceFile" to SourceFileAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("CONSTANT_Module"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_MODULE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("#8"), failure.message)
    }
    @Test
    fun `rejects package constants in ordinary classfiles`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithPackageConstantBytes(),
                source = "OrdinaryWithPackageConstant.class",
                attributeParsers = AttributeParserRegistry.of("SourceFile" to SourceFileAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("CONSTANT_Package"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_MODULE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("#8"), failure.message)
    }
    @Test
    fun `rejects module classfile before Java 9`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = moduleClassFileBeforeJava9Bytes(),
                source = "Java8ModuleInfo.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_MODULE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("major_version"), failure.message)
        assertTrue(failure.message.orEmpty().contains("53"), failure.message)
        assertTrue(failure.message.orEmpty().contains("52"), failure.message)
    }

    @Test
    fun `rejects Code attributes in ClassFile attribute table`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithClassLevelCodeBytes(),
                source = "ClassLevelCode.class",
                attributeParsers = AttributeParserRegistry.of("Code" to CodeAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("Code"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method_info"), failure.message)
    }

    @Test
    fun `rejects ConstantValue attributes in ClassFile attribute table`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithClassLevelConstantValueBytes(),
                source = "ClassLevelConstantValue.class",
                attributeParsers = AttributeParserRegistry.of("ConstantValue" to ConstantValueAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ConstantValue"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field_info"), failure.message)
    }

    @Test
    fun `rejects MethodParameters attributes in ClassFile attribute table`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithClassLevelMethodParametersBytes(),
                source = "ClassLevelMethodParameters.class",
                attributeParsers = AttributeParserRegistry.of(
                    "MethodParameters" to MethodParametersAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("MethodParameters"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method_info"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate SourceFile attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateSourceFileBytes(),
                source = "DuplicateSourceFile.class",
                attributeParsers = AttributeParserRegistry.of("SourceFile" to SourceFileAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("SourceFile"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate SourceDebugExtension attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateSourceDebugExtensionBytes(),
                source = "DuplicateSourceDebugExtension.class",
                attributeParsers = AttributeParserRegistry.of(
                    "SourceDebugExtension" to SourceDebugExtensionAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("SourceDebugExtension"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate EnclosingMethod attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateEnclosingMethodBytes(),
                source = "DuplicateEnclosingMethod.class",
                attributeParsers = AttributeParserRegistry.of("EnclosingMethod" to EnclosingMethodAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("EnclosingMethod"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate InnerClasses attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateInnerClassesBytes(),
                source = "DuplicateInnerClasses.class",
                attributeParsers = AttributeParserRegistry.of("InnerClasses" to InnerClassesAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("InnerClasses"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate Record attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateRecordBytes(),
                source = "DuplicateRecord.class",
                attributeParsers = AttributeParserRegistry.of("Record" to RecordAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("Record"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate ModuleMainClass attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateModuleMainClassBytes(),
                source = "DuplicateModuleMainClass.class",
                attributeParsers = AttributeParserRegistry.of(
                    "ModuleMainClass" to ModuleMainClassAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ModuleMainClass"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate ModulePackages attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateModulePackagesBytes(),
                source = "DuplicateModulePackages.class",
                attributeParsers = AttributeParserRegistry.of("ModulePackages" to ModulePackagesAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ModulePackages"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate Module attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateModuleBytes(),
                source = "DuplicateModule.class",
                attributeParsers = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("Module"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }


    @Test
    fun `rejects ClassFile with duplicate BootstrapMethods attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateBootstrapMethodsBytes(),
                source = "DuplicateBootstrapMethods.class",
                attributeParsers = AttributeParserRegistry.of("BootstrapMethods" to BootstrapMethodsAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("BootstrapMethods"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects dynamic constants before Java 11`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDynamicConstantBeforeJava11Bytes(),
                source = "Java10DynamicConstant.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("CONSTANT_Dynamic"), failure.message)
        assertTrue(failure.message.orEmpty().contains("major_version"), failure.message)
        assertTrue(failure.message.orEmpty().contains("55"), failure.message)
        assertTrue(failure.message.orEmpty().contains("54"), failure.message)
    }
    @Test
    fun `rejects method type constants before Java 7`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithMethodTypeBeforeJava7Bytes(),
                source = "Java6MethodTypeConstant.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("CONSTANT_MethodType"), failure.message)
        assertTrue(failure.message.orEmpty().contains("major_version"), failure.message)
        assertTrue(failure.message.orEmpty().contains("51"), failure.message)
        assertTrue(failure.message.orEmpty().contains("50"), failure.message)
    }
    @Test
    fun `rejects method handle constants before Java 7`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithMethodHandleBeforeJava7Bytes(),
                source = "Java6MethodHandleConstant.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("CONSTANT_MethodHandle"), failure.message)
        assertTrue(failure.message.orEmpty().contains("major_version"), failure.message)
        assertTrue(failure.message.orEmpty().contains("51"), failure.message)
        assertTrue(failure.message.orEmpty().contains("50"), failure.message)
    }
    @Test
    fun `rejects invokedynamic constants before Java 7`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithInvokeDynamicBeforeJava7Bytes(),
                source = "Java6InvokeDynamicConstant.class",
            )
        }

        assertTrue(failure.message.orEmpty().contains("CONSTANT_InvokeDynamic"), failure.message)
        assertTrue(failure.message.orEmpty().contains("major_version"), failure.message)
        assertTrue(failure.message.orEmpty().contains("51"), failure.message)
        assertTrue(failure.message.orEmpty().contains("50"), failure.message)
    }
    @Test
    fun `rejects ClassFile with dynamic constant but no BootstrapMethods attribute`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDynamicConstantWithoutBootstrapMethodsBytes(),
                source = "MissingBootstrapMethods.class",
                attributeParsers = AttributeParserRegistry.of("BootstrapMethods" to BootstrapMethodsAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("BootstrapMethods"), failure.message)
        assertTrue(failure.message.orEmpty().contains("exactly one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Dynamic"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate Signature attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateSignatureBytes(),
                source = "DuplicateSignature.class",
                attributeParsers = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("Signature"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate RuntimeVisibleAnnotations attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateRuntimeVisibleAnnotationsBytes(),
                source = "DuplicateRuntimeVisibleAnnotations.class",
                attributeParsers = AttributeParserRegistry.of(
                    "RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("RuntimeVisibleAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate RuntimeInvisibleAnnotations attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateRuntimeInvisibleAnnotationsBytes(),
                source = "DuplicateRuntimeInvisibleAnnotations.class",
                attributeParsers = AttributeParserRegistry.of(
                    "RuntimeInvisibleAnnotations" to RuntimeInvisibleAnnotationsAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("RuntimeInvisibleAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate RuntimeVisibleTypeAnnotations attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateRuntimeVisibleTypeAnnotationsBytes(),
                source = "DuplicateRuntimeVisibleTypeAnnotations.class",
                attributeParsers = AttributeParserRegistry.of(
                    "RuntimeVisibleTypeAnnotations" to RuntimeVisibleTypeAnnotationsAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("RuntimeVisibleTypeAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate RuntimeInvisibleTypeAnnotations attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateRuntimeInvisibleTypeAnnotationsBytes(),
                source = "DuplicateRuntimeInvisibleTypeAnnotations.class",
                attributeParsers = AttributeParserRegistry.of(
                    "RuntimeInvisibleTypeAnnotations" to RuntimeInvisibleTypeAnnotationsAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("RuntimeInvisibleTypeAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with both NestHost and NestMembers attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithNestHostAndNestMembersBytes(),
                source = "ConflictingNest.class",
                attributeParsers = AttributeParserRegistry.of(
                    "NestHost" to NestHostAttributeParser,
                    "NestMembers" to NestMembersAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("NestHost"), failure.message)
        assertTrue(failure.message.orEmpty().contains("NestMembers"), failure.message)
        assertTrue(failure.message.orEmpty().contains("must not contain both"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate NestHost attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateNestHostBytes(),
                source = "DuplicateNestHost.class",
                attributeParsers = AttributeParserRegistry.of("NestHost" to NestHostAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("NestHost"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate NestMembers attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateNestMembersBytes(),
                source = "DuplicateNestMembers.class",
                attributeParsers = AttributeParserRegistry.of("NestMembers" to NestMembersAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("NestMembers"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate PermittedSubclasses attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicatePermittedSubclassesBytes(),
                source = "DuplicatePermittedSubclasses.class",
                attributeParsers = AttributeParserRegistry.of(
                    "PermittedSubclasses" to PermittedSubclassesAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("PermittedSubclasses"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects final ClassFile with PermittedSubclasses attribute`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = finalClassFileWithPermittedSubclassesBytes(),
                source = "FinalPermittedSubclasses.class",
                attributeParsers = AttributeParserRegistry.of(
                    "PermittedSubclasses" to PermittedSubclassesAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_FINAL"), failure.message)
        assertTrue(failure.message.orEmpty().contains("PermittedSubclasses"), failure.message)
        assertTrue(failure.message.orEmpty().contains("must not declare"), failure.message)
    }

    private fun minimalClassFileBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 7,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 10,
            'S'.code, 'o'.code, 'u'.code, 'r'.code, 'c'.code, 'e'.code, 'F'.code, 'i'.code, 'l'.code, 'e'.code,
            1, 0, 9,
            'T'.code, 'e'.code, 's'.code, 't'.code, '.'.code, 'j'.code, 'a'.code, 'v'.code, 'a'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 1,
            0, 5,
            0, 0, 0, 2,
            0, 6,
        )

    private fun classFileWithModuleConstantBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 9,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 10,
            'S'.code, 'o'.code, 'u'.code, 'r'.code, 'c'.code, 'e'.code, 'F'.code, 'i'.code, 'l'.code, 'e'.code,
            1, 0, 9,
            'T'.code, 'e'.code, 's'.code, 't'.code, '.'.code, 'j'.code, 'a'.code, 'v'.code, 'a'.code,
            1, 0, 6, 'f'.code, 'r'.code, 'i'.code, 'e'.code, 'n'.code, 'd'.code,
            19, 0, 7,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 1,
            0, 5,
            0, 0, 0, 2,
            0, 6,
        )
    private fun classFileWithPackageConstantBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 9,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 10,
            'S'.code, 'o'.code, 'u'.code, 'r'.code, 'c'.code, 'e'.code, 'F'.code, 'i'.code, 'l'.code, 'e'.code,
            1, 0, 9,
            'T'.code, 'e'.code, 's'.code, 't'.code, '.'.code, 'j'.code, 'a'.code, 'v'.code, 'a'.code,
            1, 0, 3, 'p'.code, 'k'.code, 'g'.code,
            20, 0, 7,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 1,
            0, 5,
            0, 0, 0, 2,
            0, 6,
        )
    private fun classFileWithNonClassThisClassBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            1, 0, 4, 'S'.code, 'e'.code, 'l'.code, 'f'.code,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun classFileWithArrayThisClassBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 19,
            '['.code, 'L'.code, 'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code, ';'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun classFileWithNonClassSuperClassBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            1, 0, 8, 'N'.code, 'o'.code, 't'.code, 'C'.code, 'l'.code, 'a'.code, 's'.code, 's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun classFileWithArraySuperClassBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 19,
            '['.code, 'L'.code, 'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code, ';'.code,
            7, 0, 3,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun classFileWithNonClassInterfaceBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 8, 'N'.code, 'o'.code, 't'.code, 'C'.code, 'l'.code, 'a'.code, 's'.code, 's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 1,
            0, 5,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun classFileWithArrayInterfaceBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 7,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 19,
            '['.code, 'L'.code, 'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code, ';'.code,
            7, 0, 5,
            0, 0x21,
            0, 2,
            0, 4,
            0, 1,
            0, 6,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun classFileWithDuplicateInterfaceIndexesBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            0, 0x21,
            0, 2,
            0, 4,
            0, 2,
            0, 4,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun classFileWithDuplicateInterfaceNamesBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 7,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 5,
            0, 0x21,
            0, 2,
            0, 4,
            0, 2,
            0, 4,
            0, 6,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun classFileWithZeroSuperNonObjectBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 3,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            0, 0x21,
            0, 2,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun objectClassFileWithNonzeroSuperBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 1,
            1, 0, 10, 'O'.code, 't'.code, 'h'.code, 'e'.code, 'r'.code,
            'S'.code, 'u'.code, 'p'.code, 'e'.code, 'r'.code,
            7, 0, 3,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun interfaceClassFileWithNonObjectSuperBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 11, 'o'.code, 't'.code, 'h'.code, 'e'.code, 'r'.code,
            '/'.code, 'S'.code, 'u'.code, 'p'.code, 'e'.code, 'r'.code,
            7, 0, 3,
            0x06, 0x01,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun moduleClassFileWithNonModuleInfoThisClassBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 3,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            0x80, 0x00,
            0, 2,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun moduleClassFileWithNonzeroSuperBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 11,
            'm'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            '-'.code, 'i'.code, 'n'.code, 'f'.code, 'o'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            0x80, 0x00,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun moduleClassFileWithInterfaceBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 11,
            'm'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            '-'.code, 'i'.code, 'n'.code, 'f'.code, 'o'.code,
            7, 0, 1,
            1, 0, 18,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'R'.code, 'u'.code, 'n'.code, 'n'.code, 'a'.code, 'b'.code, 'l'.code, 'e'.code,
            7, 0, 3,
            0x80, 0x00,
            0, 2,
            0, 0,
            0, 1,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun moduleClassFileWithFieldBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 11,
            'm'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            '-'.code, 'i'.code, 'n'.code, 'f'.code, 'o'.code,
            7, 0, 1,
            1, 0, 1, 'x'.code,
            1, 0, 1, 'I'.code,
            0x80, 0x00,
            0, 2,
            0, 0,
            0, 0,
            0, 1,
            0, 0,
            0, 3,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun moduleClassFileWithMethodBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 11,
            'm'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            '-'.code, 'i'.code, 'n'.code, 'f'.code, 'o'.code,
            7, 0, 1,
            1, 0, 1, 'm'.code,
            1, 0, 3, '('.code, ')'.code, 'V'.code,
            0x80, 0x00,
            0, 2,
            0, 0,
            0, 0,
            0, 0,
            0, 1,
            0x01, 0x00,
            0, 3,
            0, 4,
            0, 0,
            0, 0,
        )

    private fun moduleClassFileWithoutModuleAttributeBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 3,
            1, 0, 11,
            'm'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            '-'.code, 'i'.code, 'n'.code, 'f'.code, 'o'.code,
            7, 0, 1,
            0x80, 0x00,
            0, 2,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun moduleClassFileWithBootstrapMethodsBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 5,
            1, 0, 11,
            'm'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            '-'.code, 'i'.code, 'n'.code, 'f'.code, 'o'.code,
            7, 0, 1,
            1, 0, 6, 'M'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            1, 0, 16,
            'B'.code, 'o'.code, 'o'.code, 't'.code, 's'.code, 't'.code, 'r'.code, 'a'.code,
            'p'.code, 'M'.code, 'e'.code, 't'.code, 'h'.code, 'o'.code, 'd'.code, 's'.code,
            0x80, 0x00,
            0, 2,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 3,
            0, 0, 0, 0,
            0, 4,
            0, 0, 0, 0,
        )

    private fun moduleClassFileBeforeJava9Bytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 52,
            0, 4,
            1, 0, 11,
            'm'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            '-'.code, 'i'.code, 'n'.code, 'f'.code, 'o'.code,
            7, 0, 1,
            1, 0, 6, 'M'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            0x80, 0x00,
            0, 2,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 1,
            0, 3,
            0, 0, 0, 0,
        )

    private fun classFileWithClassLevelCodeBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 4, 'C'.code, 'o'.code, 'd'.code, 'e'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 1,
            0, 5,
            0, 0, 0, 13,
            0, 0,
            0, 0,
            0, 0, 0, 1,
            0xB1,
            0, 0,
            0, 0,
        )

    private fun classFileWithClassLevelConstantValueBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 7,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 13,
            'C'.code, 'o'.code, 'n'.code, 's'.code, 't'.code, 'a'.code, 'n'.code,
            't'.code, 'V'.code, 'a'.code, 'l'.code, 'u'.code, 'e'.code,
            3, 0, 0, 0, 1,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 1,
            0, 5,
            0, 0, 0, 2,
            0, 6,
        )

    private fun classFileWithClassLevelMethodParametersBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 16,
            'M'.code, 'e'.code, 't'.code, 'h'.code, 'o'.code, 'd'.code, 'P'.code, 'a'.code,
            'r'.code, 'a'.code, 'm'.code, 'e'.code, 't'.code, 'e'.code, 'r'.code, 's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 1,
            0, 5,
            0, 0, 0, 1,
            0,
        )

    private fun classFileWithDuplicateSourceDebugExtensionBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 20,
            'S'.code, 'o'.code, 'u'.code, 'r'.code, 'c'.code, 'e'.code, 'D'.code, 'e'.code, 'b'.code, 'u'.code,
            'g'.code, 'E'.code, 'x'.code, 't'.code, 'e'.code, 'n'.code, 's'.code, 'i'.code, 'o'.code, 'n'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 1,
            'A'.code,
            0, 5,
            0, 0, 0, 1,
            'B'.code,
        )

    private fun classFileWithDuplicateEnclosingMethodBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 15,
            'E'.code, 'n'.code, 'c'.code, 'l'.code, 'o'.code, 's'.code, 'i'.code, 'n'.code, 'g'.code,
            'M'.code, 'e'.code, 't'.code, 'h'.code, 'o'.code, 'd'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 4,
            0, 4,
            0, 0,
            0, 5,
            0, 0, 0, 4,
            0, 4,
            0, 0,
        )

    private fun classFileWithDuplicateInnerClassesBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 12,
            'I'.code, 'n'.code, 'n'.code, 'e'.code, 'r'.code, 'C'.code, 'l'.code, 'a'.code, 's'.code, 's'.code,
            'e'.code, 's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
        )

    private fun classFileWithDuplicateRecordBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 6, 'R'.code, 'e'.code, 'c'.code, 'o'.code, 'r'.code, 'd'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
        )

    private fun classFileWithDuplicateModuleMainClassBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 15,
            'M'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code, 'M'.code, 'a'.code, 'i'.code, 'n'.code,
            'C'.code, 'l'.code, 'a'.code, 's'.code, 's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 2,
        )

    private fun classFileWithDuplicateModulePackagesBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 14,
            'M'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code, 'P'.code, 'a'.code, 'c'.code, 'k'.code,
            'a'.code, 'g'.code, 'e'.code, 's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
        )

    private fun classFileWithDuplicateModuleBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 8,
            1, 0, 11,
            'm'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            '-'.code, 'i'.code, 'n'.code, 'f'.code, 'o'.code,
            7, 0, 1,
            1, 0, 6, 'M'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            1, 0, 9,
            'm'.code, 'y'.code, '.'.code, 'm'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            19, 0, 4,
            1, 0, 9,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '.'.code, 'b'.code, 'a'.code, 's'.code, 'e'.code,
            19, 0, 6,
            0x80, 0x00,
            0, 2,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 3,
            0, 0, 0, 22,
            0, 5,
            0, 0,
            0, 0,
            0, 1,
            0, 7,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 3,
            0, 0, 0, 22,
            0, 5,
            0, 0,
            0, 0,
            0, 1,
            0, 7,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )
    private fun classFileWithDuplicateSignatureBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 7,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 9, 'S'.code, 'i'.code, 'g'.code, 'n'.code, 'a'.code, 't'.code, 'u'.code, 'r'.code, 'e'.code,
            1, 0, 18,
            'L'.code, 'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code, 'l'.code, 'a'.code, 'n'.code, 'g'.code,
            '/'.code, 'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code, ';'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 6,
            0, 5,
            0, 0, 0, 2,
            0, 6,
        )

    private fun classFileWithDuplicateSourceFileBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 7,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 10,
            'S'.code, 'o'.code, 'u'.code, 'r'.code, 'c'.code, 'e'.code, 'F'.code, 'i'.code, 'l'.code, 'e'.code,
            1, 0, 9,
            'T'.code, 'e'.code, 's'.code, 't'.code, '.'.code, 'j'.code, 'a'.code, 'v'.code, 'a'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 6,
            0, 5,
            0, 0, 0, 2,
            0, 6,
        )


    private fun classFileWithDynamicConstantWithoutBootstrapMethodsBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 9,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 5, 'v'.code, 'a'.code, 'l'.code, 'u'.code, 'e'.code,
            1, 0, 1, 'I'.code,
            12, 0, 5, 0, 6,
            17, 0, 0, 0, 7,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun classFileWithMethodTypeBeforeJava7Bytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 50,
            0, 7,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 3, '('.code, ')'.code, 'V'.code,
            16, 0, 5,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )
    private fun classFileWithMethodHandleBeforeJava7Bytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 50,
            0, 10,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 5, 'f'.code, 'i'.code, 'e'.code, 'l'.code, 'd'.code,
            1, 0, 1, 'I'.code,
            12, 0, 5, 0, 6,
            9, 0, 2, 0, 7,
            15, 1, 0, 8,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )
    private fun classFileWithInvokeDynamicBeforeJava7Bytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 50,
            0, 9,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 3, 'r'.code, 'u'.code, 'n'.code,
            1, 0, 3, '('.code, ')'.code, 'V'.code,
            12, 0, 5, 0, 6,
            18, 0, 0, 0, 7,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )
    private fun classFileWithDynamicConstantBeforeJava11Bytes(): ByteArray =
        classFileWithDynamicConstantWithoutBootstrapMethodsBytes().also { bytes ->
            bytes[7] = 54
        }
    private fun classFileWithDuplicateBootstrapMethodsBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 16,
            'B'.code, 'o'.code, 'o'.code, 't'.code, 's'.code, 't'.code, 'r'.code, 'a'.code, 'p'.code,
            'M'.code, 'e'.code, 't'.code, 'h'.code, 'o'.code, 'd'.code, 's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
        )
    private fun classFileWithDuplicateRuntimeVisibleAnnotationsBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 25,
            'R'.code, 'u'.code, 'n'.code, 't'.code, 'i'.code, 'm'.code, 'e'.code,
            'V'.code, 'i'.code, 's'.code, 'i'.code, 'b'.code, 'l'.code, 'e'.code,
            'A'.code, 'n'.code, 'n'.code, 'o'.code, 't'.code, 'a'.code, 't'.code, 'i'.code, 'o'.code, 'n'.code,
            's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
        )

    private fun classFileWithDuplicateRuntimeInvisibleAnnotationsBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 27,
            'R'.code, 'u'.code, 'n'.code, 't'.code, 'i'.code, 'm'.code, 'e'.code,
            'I'.code, 'n'.code, 'v'.code, 'i'.code, 's'.code, 'i'.code, 'b'.code, 'l'.code, 'e'.code,
            'A'.code, 'n'.code, 'n'.code, 'o'.code, 't'.code, 'a'.code, 't'.code, 'i'.code, 'o'.code, 'n'.code,
            's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
        )

    private fun classFileWithDuplicateRuntimeVisibleTypeAnnotationsBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 29,
            'R'.code, 'u'.code, 'n'.code, 't'.code, 'i'.code, 'm'.code, 'e'.code,
            'V'.code, 'i'.code, 's'.code, 'i'.code, 'b'.code, 'l'.code, 'e'.code,
            'T'.code, 'y'.code, 'p'.code, 'e'.code,
            'A'.code, 'n'.code, 'n'.code, 'o'.code, 't'.code, 'a'.code, 't'.code, 'i'.code, 'o'.code, 'n'.code,
            's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
        )

    private fun classFileWithDuplicateRuntimeInvisibleTypeAnnotationsBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 31,
            'R'.code, 'u'.code, 'n'.code, 't'.code, 'i'.code, 'm'.code, 'e'.code,
            'I'.code, 'n'.code, 'v'.code, 'i'.code, 's'.code, 'i'.code, 'b'.code, 'l'.code, 'e'.code,
            'T'.code, 'y'.code, 'p'.code, 'e'.code,
            'A'.code, 'n'.code, 'n'.code, 'o'.code, 't'.code, 'a'.code, 't'.code, 'i'.code, 'o'.code, 'n'.code,
            's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
        )

    private fun classFileWithNestHostAndNestMembersBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 9,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 8,
            'N'.code, 'e'.code, 's'.code, 't'.code, 'H'.code, 'o'.code, 's'.code, 't'.code,
            1, 0, 11,
            'N'.code, 'e'.code, 's'.code, 't'.code, 'M'.code, 'e'.code, 'm'.code, 'b'.code, 'e'.code, 'r'.code,
            's'.code,
            1, 0, 10,
            'p'.code, 'k'.code, 'g'.code, '/'.code, 'M'.code, 'e'.code, 'm'.code, 'b'.code, 'e'.code, 'r'.code,
            7, 0, 9,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 2,
            0, 6,
            0, 0, 0, 4,
            0, 1,
            0, 8,
        )

    private fun classFileWithDuplicateNestHostBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 8,
            'N'.code, 'e'.code, 's'.code, 't'.code, 'H'.code, 'o'.code, 's'.code, 't'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 2,
        )

    private fun classFileWithDuplicateNestMembersBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 8,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 11,
            'N'.code, 'e'.code, 's'.code, 't'.code, 'M'.code, 'e'.code, 'm'.code, 'b'.code, 'e'.code, 'r'.code,
            's'.code,
            1, 0, 10,
            'p'.code, 'k'.code, 'g'.code, '/'.code, 'M'.code, 'e'.code, 'm'.code, 'b'.code, 'e'.code, 'r'.code,
            7, 0, 6,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 4,
            0, 1,
            0, 7,
            0, 5,
            0, 0, 0, 4,
            0, 1,
            0, 7,
        )

    private fun classFileWithDuplicatePermittedSubclassesBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 8,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 19,
            'P'.code, 'e'.code, 'r'.code, 'm'.code, 'i'.code, 't'.code, 't'.code, 'e'.code, 'd'.code,
            'S'.code, 'u'.code, 'b'.code, 'c'.code, 'l'.code, 'a'.code, 's'.code, 's'.code, 'e'.code, 's'.code,
            1, 0, 10,
            'p'.code, 'k'.code, 'g'.code, '/'.code, 'M'.code, 'e'.code, 'm'.code, 'b'.code, 'e'.code, 'r'.code,
            7, 0, 6,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 4,
            0, 1,
            0, 7,
            0, 5,
            0, 0, 0, 4,
            0, 1,
            0, 7,
        )

    private fun finalClassFileWithPermittedSubclassesBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 8,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 19,
            'P'.code, 'e'.code, 'r'.code, 'm'.code, 'i'.code, 't'.code, 't'.code, 'e'.code, 'd'.code,
            'S'.code, 'u'.code, 'b'.code, 'c'.code, 'l'.code, 'a'.code, 's'.code, 's'.code, 'e'.code, 's'.code,
            1, 0, 10,
            'p'.code, 'k'.code, 'g'.code, '/'.code, 'M'.code, 'e'.code, 'm'.code, 'b'.code, 'e'.code, 'r'.code,
            7, 0, 6,
            0, 0x31,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 1,
            0, 5,
            0, 0, 0, 4,
            0, 1,
            0, 7,
        )

    private fun bytes(vararg values: Int): ByteArray =
        values.map { it.toByte() }.toByteArray()
}
