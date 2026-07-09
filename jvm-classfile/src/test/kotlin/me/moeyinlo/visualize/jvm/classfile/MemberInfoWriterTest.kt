package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun `rejects non-simple member attributes until their specific writer is available`() {
        val failure = assertFailsWith<UnsupportedOperationException> {
            ClassFileWriter.writeFields(
                listOf(
                    FieldInfo(
                        accessFlags = 0x0001,
                        nameIndex = ConstantPoolIndex(3),
                        descriptorIndex = ConstantPoolIndex(4),
                        attributes = listOf(ConstantValueAttribute(ConstantPoolIndex(5), ConstantPoolIndex(6))),
                    ),
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ConstantValueAttribute"), failure.message)
        assertTrue(failure.message.orEmpty().contains("fields[0].attributes[0]"), failure.message)
    }
}
