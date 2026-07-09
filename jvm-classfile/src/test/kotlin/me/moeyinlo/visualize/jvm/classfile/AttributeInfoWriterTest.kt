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
