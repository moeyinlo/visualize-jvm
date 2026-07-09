package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MemberInfoWriterTest {
    @Test
    fun `writes fields table with empty attribute tables`() {
        val fields = listOf(
            FieldInfo(
                accessFlags = 0x0001,
                nameIndex = ConstantPoolIndex(3),
                descriptorIndex = ConstantPoolIndex(4),
                attributes = emptyList(),
            ),
            FieldInfo(
                accessFlags = 0x0019,
                nameIndex = ConstantPoolIndex(5),
                descriptorIndex = ConstantPoolIndex(6),
                attributes = emptyList(),
            ),
        )

        val bytes = ClassFileWriter.writeFields(fields)

        assertContentEquals(
            byteArrayOf(
                0,
                2,
                0,
                1,
                0,
                3,
                0,
                4,
                0,
                0,
                0,
                0x19,
                0,
                5,
                0,
                6,
                0,
                0,
            ),
            bytes,
        )

        val parsed = FieldInfoParser.parseFields(ClassFileByteReader(bytes, source = "written-fields.class"))
        assertEquals(fields, parsed)
    }

    @Test
    fun `writes methods table with empty attribute tables`() {
        val methods = listOf(
            MethodInfo(
                accessFlags = 0x0001,
                nameIndex = ConstantPoolIndex(7),
                descriptorIndex = ConstantPoolIndex(8),
                attributes = emptyList(),
            ),
            MethodInfo(
                accessFlags = 0x0009,
                nameIndex = ConstantPoolIndex(9),
                descriptorIndex = ConstantPoolIndex(10),
                attributes = emptyList(),
            ),
        )

        val bytes = ClassFileWriter.writeMethods(methods)

        assertContentEquals(
            byteArrayOf(
                0,
                2,
                0,
                1,
                0,
                7,
                0,
                8,
                0,
                0,
                0,
                9,
                0,
                9,
                0,
                10,
                0,
                0,
            ),
            bytes,
        )

        val parsed = MethodInfoParser.parseMethods(ClassFileByteReader(bytes, source = "written-methods.class"))
        assertEquals(methods, parsed)
    }

    @Test
    fun `field writer includes ConstantValue attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ConstantValue", byteArrayOf()),
                ConstantIntegerEntry(42),
            ),
        )
        val bytes = ClassFileWriter.writeFields(
            listOf(
                FieldInfo(
                    accessFlags = 0x0019,
                    nameIndex = ConstantPoolIndex(3),
                    descriptorIndex = ConstantPoolIndex(4),
                    attributes = listOf(ConstantValueAttribute(ConstantPoolIndex(1), ConstantPoolIndex(2))),
                ),
            ),
        )

        val parsed = FieldInfoParser.parseFields(
            reader = ClassFileByteReader(bytes, source = "field-constant-value.class"),
            constantPool = constantPool,
            attributeParsers = AttributeParserRegistry.of("ConstantValue" to ConstantValueAttributeParser),
        )

        val field = parsed.single()
        assertEquals(0x0019, field.accessFlags)
        val attribute = assertIs<ConstantValueAttribute>(field.attributes.single())
        assertEquals(ConstantPoolIndex(1), attribute.nameIndex)
        assertEquals(ConstantPoolIndex(2), attribute.constantValueIndex)
    }

    @Test
    fun `method writer includes Exceptions attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Exceptions", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("java/io/IOException", byteArrayOf()),
            ),
        )
        val bytes = ClassFileWriter.writeMethods(
            listOf(
                MethodInfo(
                    accessFlags = 0x0001,
                    nameIndex = ConstantPoolIndex(4),
                    descriptorIndex = ConstantPoolIndex(5),
                    attributes = listOf(
                        ExceptionsAttribute(
                            nameIndex = ConstantPoolIndex(1),
                            exceptionIndexTable = listOf(ConstantPoolIndex(2)),
                        ),
                    ),
                ),
            ),
        )

        val parsed = MethodInfoParser.parseMethods(
            reader = ClassFileByteReader(bytes, source = "method-exceptions.class"),
            constantPool = constantPool,
            attributeParsers = AttributeParserRegistry.of("Exceptions" to ExceptionsAttributeParser),
        )

        val method = parsed.single()
        assertEquals(0x0001, method.accessFlags)
        val attribute = assertIs<ExceptionsAttribute>(method.attributes.single())
        assertEquals(ConstantPoolIndex(1), attribute.nameIndex)
        assertEquals(listOf(ConstantPoolIndex(2)), attribute.exceptionIndexTable)
    }

    @Test
    fun `rejects unsupported member attributes until their specific writer is available`() {
        val failure = assertFailsWith<UnsupportedOperationException> {
            ClassFileWriter.writeMethods(
                listOf(
                    MethodInfo(
                        accessFlags = 0x0001,
                        nameIndex = ConstantPoolIndex(3),
                        descriptorIndex = ConstantPoolIndex(4),
                        attributes = listOf(
                            CodeAttribute(
                                nameIndex = ConstantPoolIndex(5),
                                maxStack = 1,
                                maxLocals = 1,
                                code = byteArrayOf(0xB1.toByte()),
                            ),
                        ),
                    ),
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("CodeAttribute"), failure.message)
        assertTrue(failure.message.orEmpty().contains("methods[0].attributes[0]"), failure.message)
    }
}
