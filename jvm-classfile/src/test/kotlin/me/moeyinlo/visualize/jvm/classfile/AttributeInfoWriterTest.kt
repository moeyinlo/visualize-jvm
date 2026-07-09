package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AttributeInfoWriterTest {
    @Test
    fun `writes raw and unknown attributes in table order`() {
        val attributes = listOf(
            RawAttributeInfo(ConstantPoolIndex(3), byteArrayOf(1, 2, 3)),
            UnknownAttributeInfo(ConstantPoolIndex(4), "VendorAttribute", byteArrayOf(4, 5)),
        )

        val bytes = ClassFileWriter.writeAttributes(attributes)

        assertContentEquals(
            byteArrayOf(
                0,
                2,
                0,
                3,
                0,
                0,
                0,
                3,
                1,
                2,
                3,
                0,
                4,
                0,
                0,
                0,
                2,
                4,
                5,
            ),
            bytes,
        )

        val parsed = RawAttributeInfoParser.parseAttributes(
            ClassFileByteReader(bytes, source = "written-attributes.class"),
            ownerPath = "ClassFile",
        )
        assertEquals(ConstantPoolIndex(3), parsed[0].nameIndex)
        assertContentEquals(byteArrayOf(1, 2, 3), parsed[0].info)
        assertEquals(ConstantPoolIndex(4), parsed[1].nameIndex)
        assertContentEquals(byteArrayOf(4, 5), parsed[1].info)
    }

    @Test
    fun `member writers include raw attribute tables`() {
        val fieldBytes = ClassFileWriter.writeFields(
            listOf(
                FieldInfo(
                    accessFlags = 0x0001,
                    nameIndex = ConstantPoolIndex(3),
                    descriptorIndex = ConstantPoolIndex(4),
                    attributes = listOf(RawAttributeInfo(ConstantPoolIndex(5), byteArrayOf(9))),
                ),
            ),
        )

        val parsed = FieldInfoParser.parseFields(ClassFileByteReader(fieldBytes, source = "field-attribute.class"))
        val field = parsed.single()

        assertEquals(0x0001, field.accessFlags)
        assertEquals(ConstantPoolIndex(3), field.nameIndex)
        assertEquals(ConstantPoolIndex(4), field.descriptorIndex)
        val attribute = assertIs<RawAttributeInfo>(field.attributes.single())
        assertEquals(ConstantPoolIndex(5), attribute.nameIndex)
        assertContentEquals(byteArrayOf(9), attribute.info)
    }

    @Test
    fun `writes simple fixed-length attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Synthetic", byteArrayOf()),
                ConstantUtf8Entry("Deprecated", byteArrayOf()),
                ConstantUtf8Entry("SourceFile", byteArrayOf()),
                ConstantUtf8Entry("Main.java", byteArrayOf()),
            ),
        )
        val attributes = listOf(
            SyntheticAttribute(ConstantPoolIndex(1)),
            DeprecatedAttribute(ConstantPoolIndex(2)),
            SourceFileAttribute(
                nameIndex = ConstantPoolIndex(3),
                sourceFileIndex = ConstantPoolIndex(4),
            ),
        )

        val bytes = ClassFileWriter.writeAttributes(attributes)

        assertContentEquals(
            byteArrayOf(
                0,
                3,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                2,
                0,
                0,
                0,
                0,
                0,
                3,
                0,
                0,
                0,
                2,
                0,
                4,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "simple-attributes.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of(
                "Synthetic" to SyntheticAttributeParser,
                "Deprecated" to DeprecatedAttributeParser,
                "SourceFile" to SourceFileAttributeParser,
            ),
            ownerPath = "ClassFile",
        )

        assertIs<SyntheticAttribute>(parsed[0])
        assertIs<DeprecatedAttribute>(parsed[1])
        assertEquals(ConstantPoolIndex(4), assertIs<SourceFileAttribute>(parsed[2]).sourceFileIndex)
    }

    @Test
    fun `member writers include simple attributes`() {
        val methodBytes = ClassFileWriter.writeMethods(
            listOf(
                MethodInfo(
                    accessFlags = 0x0001,
                    nameIndex = ConstantPoolIndex(3),
                    descriptorIndex = ConstantPoolIndex(4),
                    attributes = listOf(SyntheticAttribute(ConstantPoolIndex(5))),
                ),
            ),
        )

        val parsed = MethodInfoParser.parseMethods(ClassFileByteReader(methodBytes, source = "method-simple.class"))

        val attribute = assertIs<RawAttributeInfo>(parsed.single().attributes.single())
        assertEquals(ConstantPoolIndex(5), attribute.nameIndex)
        assertContentEquals(byteArrayOf(), attribute.info)
    }

    @Test
    fun `writes ConstantValue attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ConstantValue", byteArrayOf()),
                ConstantIntegerEntry(42),
            ),
        )
        val attribute = ConstantValueAttribute(
            nameIndex = ConstantPoolIndex(1),
            constantValueIndex = ConstantPoolIndex(2),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                2,
                0,
                2,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "constant-value-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("ConstantValue" to ConstantValueAttributeParser),
            ownerPath = "fields[0]",
        )

        assertEquals(ConstantPoolIndex(2), assertIs<ConstantValueAttribute>(parsed.single()).constantValueIndex)
    }

    @Test
    fun `writes Exceptions attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Exceptions", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("java/io/IOException", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(5)),
                ConstantUtf8Entry("java/lang/ReflectiveOperationException", byteArrayOf()),
            ),
        )
        val attribute = ExceptionsAttribute(
            nameIndex = ConstantPoolIndex(1),
            exceptionIndexTable = listOf(ConstantPoolIndex(2), ConstantPoolIndex(4)),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                6,
                0,
                2,
                0,
                2,
                0,
                4,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "exceptions-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Exceptions" to ExceptionsAttributeParser),
            ownerPath = "methods[0]",
        )

        assertEquals(
            listOf(ConstantPoolIndex(2), ConstantPoolIndex(4)),
            assertIs<ExceptionsAttribute>(parsed.single()).exceptionIndexTable,
        )
    }

    @Test
    fun `writes InnerClasses attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("InnerClasses", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("pkg/Outer\$Inner", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(5)),
                ConstantUtf8Entry("pkg/Outer", byteArrayOf()),
                ConstantUtf8Entry("Inner", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(8)),
                ConstantUtf8Entry("pkg/Outer\$1", byteArrayOf()),
            ),
        )
        val attribute = InnerClassesAttribute(
            nameIndex = ConstantPoolIndex(1),
            classes = listOf(
                InnerClassEntry(
                    innerClassInfoIndex = ConstantPoolIndex(2),
                    outerClassInfoIndex = ConstantPoolIndex(4),
                    innerNameIndex = ConstantPoolIndex(6),
                    innerClassAccessFlags = 0x0009,
                ),
                InnerClassEntry(
                    innerClassInfoIndex = ConstantPoolIndex(7),
                    outerClassInfoIndex = null,
                    innerNameIndex = null,
                    innerClassAccessFlags = 0,
                ),
            ),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                18,
                0,
                2,
                0,
                2,
                0,
                4,
                0,
                6,
                0,
                9,
                0,
                7,
                0,
                0,
                0,
                0,
                0,
                0,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "inner-classes-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("InnerClasses" to InnerClassesAttributeParser),
            ownerPath = "ClassFile",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes EnclosingMethod attribute with absent method index`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("EnclosingMethod", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("pkg/Outer", byteArrayOf()),
            ),
        )
        val attribute = EnclosingMethodAttribute(
            nameIndex = ConstantPoolIndex(1),
            classIndex = ConstantPoolIndex(2),
            methodIndex = null,
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                4,
                0,
                2,
                0,
                0,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "enclosing-method-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("EnclosingMethod" to EnclosingMethodAttributeParser),
            ownerPath = "ClassFile",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes Signature attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantUtf8Entry("Ljava/util/List<Ljava/lang/String;>;", byteArrayOf()),
            ),
        )
        val attribute = SignatureAttribute(
            nameIndex = ConstantPoolIndex(1),
            signatureIndex = ConstantPoolIndex(2),
            signature = "Ljava/util/List<Ljava/lang/String;>;",
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                2,
                0,
                2,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "signature-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
            ownerPath = "fields[0]",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes BootstrapMethods attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("BootstrapMethods", byteArrayOf()),
                ConstantUtf8Entry("java/lang/Object", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(2)),
                ConstantUtf8Entry("()V", byteArrayOf()),
                ConstantMethodTypeEntry(ConstantPoolIndex(4)),
                ConstantMethodHandleEntry(MethodHandleReferenceKind.InvokeStatic, ConstantPoolIndex(3)),
                ConstantUtf8Entry("arg", byteArrayOf()),
                ConstantStringEntry(ConstantPoolIndex(7)),
            ),
        )
        val attribute = BootstrapMethodsAttribute(
            nameIndex = ConstantPoolIndex(1),
            bootstrapMethods = listOf(
                BootstrapMethodSpecifier(
                    bootstrapMethodRef = ConstantPoolIndex(6),
                    bootstrapArguments = listOf(ConstantPoolIndex(3), ConstantPoolIndex(5), ConstantPoolIndex(8)),
                ),
            ),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                12,
                0,
                1,
                0,
                6,
                0,
                3,
                0,
                3,
                0,
                5,
                0,
                8,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "bootstrap-methods-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("BootstrapMethods" to BootstrapMethodsAttributeParser),
            ownerPath = "ClassFile",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes SourceDebugExtension attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("SourceDebugExtension", byteArrayOf()),
            ),
        )
        val debugBytes = "SMAP\nMain.kt\nKotlin\n".encodeToByteArray()
        val attribute = SourceDebugExtensionAttribute(
            nameIndex = ConstantPoolIndex(1),
            debugExtension = debugBytes,
            text = "SMAP\nMain.kt\nKotlin\n",
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                debugBytes.size.toByte(),
            ) + debugBytes,
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "source-debug-extension-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("SourceDebugExtension" to SourceDebugExtensionAttributeParser),
            ownerPath = "ClassFile",
        )

        val parsedAttribute = assertIs<SourceDebugExtensionAttribute>(parsed.single())
        assertContentEquals(debugBytes, parsedAttribute.debugExtension)
        assertEquals("SMAP\nMain.kt\nKotlin\n", parsedAttribute.text)
    }

    @Test
    fun `writes LineNumberTable attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LineNumberTable", byteArrayOf()),
            ),
        )
        val attribute = LineNumberTableAttribute(
            nameIndex = ConstantPoolIndex(1),
            entries = listOf(
                LineNumberTableEntry(startPc = 0, lineNumber = 10),
                LineNumberTableEntry(startPc = 5, lineNumber = 20),
            ),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                10,
                0,
                2,
                0,
                0,
                0,
                10,
                0,
                5,
                0,
                20,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "line-number-table-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("LineNumberTable" to LineNumberTableAttributeParser),
            ownerPath = "methods[0].attributes[0]",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes LocalVariableTable attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LocalVariableTable", byteArrayOf()),
                ConstantUtf8Entry("arg", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
            ),
        )
        val attribute = LocalVariableTableAttribute(
            nameIndex = ConstantPoolIndex(1),
            entries = listOf(
                LocalVariableTableEntry(
                    startPc = 0,
                    length = 5,
                    nameIndex = ConstantPoolIndex(2),
                    descriptorIndex = ConstantPoolIndex(3),
                    index = 1,
                ),
            ),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                12,
                0,
                1,
                0,
                0,
                0,
                5,
                0,
                2,
                0,
                3,
                0,
                1,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "local-variable-table-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("LocalVariableTable" to LocalVariableTableAttributeParser),
            ownerPath = "methods[0].attributes[0]",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes LocalVariableTypeTable attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LocalVariableTypeTable", byteArrayOf()),
                ConstantUtf8Entry("list", byteArrayOf()),
                ConstantUtf8Entry("Ljava/util/List<Ljava/lang/String;>;", byteArrayOf()),
            ),
        )
        val attribute = LocalVariableTypeTableAttribute(
            nameIndex = ConstantPoolIndex(1),
            entries = listOf(
                LocalVariableTypeTableEntry(
                    startPc = 0,
                    length = 7,
                    nameIndex = ConstantPoolIndex(2),
                    signatureIndex = ConstantPoolIndex(3),
                    index = 2,
                ),
            ),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                12,
                0,
                1,
                0,
                0,
                0,
                7,
                0,
                2,
                0,
                3,
                0,
                2,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "local-variable-type-table-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("LocalVariableTypeTable" to LocalVariableTypeTableAttributeParser),
            ownerPath = "methods[0].attributes[0]",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes MethodParameters attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("MethodParameters", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
            ),
        )
        val attribute = MethodParametersAttribute(
            nameIndex = ConstantPoolIndex(1),
            parameters = listOf(
                MethodParameter(nameIndex = ConstantPoolIndex(2), accessFlags = 0x9010),
                MethodParameter(nameIndex = null, accessFlags = 0),
            ),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                9,
                2,
                0,
                2,
                0x90.toByte(),
                0x10,
                0,
                0,
                0,
                0,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "method-parameters-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("MethodParameters" to MethodParametersAttributeParser),
            ownerPath = "methods[0]",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes ModulePackages attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ModulePackages", byteArrayOf()),
                ConstantUtf8Entry("pkg/one", byteArrayOf()),
                ConstantPackageEntry(ConstantPoolIndex(2)),
                ConstantUtf8Entry("pkg/two", byteArrayOf()),
                ConstantPackageEntry(ConstantPoolIndex(4)),
            ),
        )
        val attribute = ModulePackagesAttribute(
            nameIndex = ConstantPoolIndex(1),
            packageIndexes = listOf(ConstantPoolIndex(3), ConstantPoolIndex(5)),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                6,
                0,
                2,
                0,
                3,
                0,
                5,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "module-packages-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("ModulePackages" to ModulePackagesAttributeParser),
            ownerPath = "ClassFile",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes ModuleMainClass attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ModuleMainClass", byteArrayOf()),
                ConstantUtf8Entry("app/Main", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(2)),
            ),
        )
        val attribute = ModuleMainClassAttribute(
            nameIndex = ConstantPoolIndex(1),
            mainClassIndex = ConstantPoolIndex(3),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                2,
                0,
                3,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "module-main-class-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("ModuleMainClass" to ModuleMainClassAttributeParser),
            ownerPath = "ClassFile",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes NestHost attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("NestHost", byteArrayOf()),
                ConstantUtf8Entry("pkg/Host", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(2)),
            ),
        )
        val attribute = NestHostAttribute(
            nameIndex = ConstantPoolIndex(1),
            hostClassIndex = ConstantPoolIndex(3),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                2,
                0,
                3,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "nest-host-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("NestHost" to NestHostAttributeParser),
            ownerPath = "ClassFile",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes NestMembers attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("NestMembers", byteArrayOf()),
                ConstantUtf8Entry("pkg/MemberOne", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(2)),
                ConstantUtf8Entry("pkg/MemberTwo", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(4)),
            ),
        )
        val attribute = NestMembersAttribute(
            nameIndex = ConstantPoolIndex(1),
            classes = listOf(ConstantPoolIndex(3), ConstantPoolIndex(5)),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                6,
                0,
                2,
                0,
                3,
                0,
                5,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "nest-members-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("NestMembers" to NestMembersAttributeParser),
            ownerPath = "ClassFile",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes PermittedSubclasses attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("PermittedSubclasses", byteArrayOf()),
                ConstantUtf8Entry("pkg/AllowedOne", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(2)),
                ConstantUtf8Entry("pkg/AllowedTwo", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(4)),
            ),
        )
        val attribute = PermittedSubclassesAttribute(
            nameIndex = ConstantPoolIndex(1),
            classes = listOf(ConstantPoolIndex(3), ConstantPoolIndex(5)),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                6,
                0,
                2,
                0,
                3,
                0,
                5,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "permitted-subclasses-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("PermittedSubclasses" to PermittedSubclassesAttributeParser),
            ownerPath = "ClassFile",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `writes Record attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("names", byteArrayOf()),
                ConstantUtf8Entry("Ljava/util/List;", byteArrayOf()),
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantUtf8Entry("Ljava/util/List<Ljava/lang/String;>;", byteArrayOf()),
            ),
        )
        val attribute = RecordAttribute(
            nameIndex = ConstantPoolIndex(1),
            components = listOf(
                RecordComponentInfo(
                    nameIndex = ConstantPoolIndex(2),
                    descriptorIndex = ConstantPoolIndex(3),
                    attributes = listOf(
                        SignatureAttribute(
                            nameIndex = ConstantPoolIndex(4),
                            signatureIndex = ConstantPoolIndex(5),
                            signature = "Ljava/util/List<Ljava/lang/String;>;",
                        ),
                    ),
                ),
            ),
        )

        val bytes = ClassFileWriter.writeAttributes(listOf(attribute))

        assertContentEquals(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                16,
                0,
                1,
                0,
                2,
                0,
                3,
                0,
                1,
                0,
                4,
                0,
                0,
                0,
                2,
                0,
                5,
            ),
            bytes,
        )

        val parsed = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(bytes, source = "record-attribute.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of(
                "Record" to RecordAttributeParser,
                "Signature" to SignatureAttributeParser,
            ),
            ownerPath = "ClassFile",
        )

        assertEquals(attribute, parsed.single())
    }

    @Test
    fun `rejects unsupported known attributes until their specific writer is implemented`() {
        val failure = assertFailsWith<UnsupportedOperationException> {
            ClassFileWriter.writeAttributes(
                listOf(
                    CodeAttribute(
                        nameIndex = ConstantPoolIndex(1),
                        maxStack = 1,
                        maxLocals = 1,
                        code = byteArrayOf(0xB1.toByte()),
                    ),
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("CodeAttribute"), failure.message)
    }
}
