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
    fun `rejects known attributes until their specific writer is implemented`() {
        val failure = assertFailsWith<UnsupportedOperationException> {
            ClassFileWriter.writeAttributes(listOf(SyntheticAttribute(ConstantPoolIndex(1))))
        }

        assertTrue(failure.message.orEmpty().contains("SyntheticAttribute"), failure.message)
    }
}
