package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ConstantPoolWriterTest {
    @Test
    fun `writes constant pool count entries and wide slot gaps`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                utf8("Example"),
                ConstantClassEntry(ConstantPoolIndex(1)),
                utf8("field"),
                utf8("I"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(4)),
                ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(5)),
                ConstantIntegerEntry(-42),
                ConstantFloatEntry(3.5f),
                ConstantLongEntry(-42L),
                ConstantDoubleEntry(1.5),
                ConstantStringEntry(ConstantPoolIndex(1)),
                utf8("()V"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(14)),
                ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(15)),
                ConstantInterfaceMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(15)),
                ConstantMethodHandleEntry(MethodHandleReferenceKind.InvokeStatic, ConstantPoolIndex(16)),
                ConstantMethodTypeEntry(ConstantPoolIndex(14)),
                ConstantDynamicEntry(BootstrapMethodIndex(0), ConstantPoolIndex(5)),
                ConstantInvokeDynamicEntry(BootstrapMethodIndex(1), ConstantPoolIndex(15)),
                utf8("mymodule"),
                ConstantModuleEntry(ConstantPoolIndex(22)),
                utf8("my/pkg"),
                ConstantPackageEntry(ConstantPoolIndex(24)),
            ),
        )

        val bytes = ClassFileWriter.writeConstantPool(constantPool)

        assertEquals(0, bytes[0].toInt())
        assertEquals(26, bytes[1].toInt())

        val parsed = ConstantPoolParser.parse(ClassFileByteReader(bytes, source = "written-pool.class"))

        assertEquals(26, parsed.constantPoolCount)
        assertEquals("Example", assertIs<ConstantUtf8Entry>(parsed[ConstantPoolIndex(1)]).value)
        assertEquals(ConstantPoolIndex(1), assertIs<ConstantClassEntry>(parsed[ConstantPoolIndex(2)]).nameIndex)
        assertEquals(-42, assertIs<ConstantIntegerEntry>(parsed[ConstantPoolIndex(7)]).value)
        assertEquals(3.5f, assertIs<ConstantFloatEntry>(parsed[ConstantPoolIndex(8)]).value)
        assertEquals(-42L, assertIs<ConstantLongEntry>(parsed[ConstantPoolIndex(9)]).value)
        assertIs<ConstantPoolSlot.Unusable>(parsed.slotAt(ConstantPoolIndex(10)))
        assertEquals(1.5, assertIs<ConstantDoubleEntry>(parsed[ConstantPoolIndex(11)]).value)
        assertIs<ConstantPoolSlot.Unusable>(parsed.slotAt(ConstantPoolIndex(12)))
        assertEquals(ConstantPoolIndex(1), assertIs<ConstantStringEntry>(parsed[ConstantPoolIndex(13)]).stringIndex)
        assertEquals(ConstantPoolIndex(16), assertIs<ConstantMethodHandleEntry>(parsed[ConstantPoolIndex(18)]).referenceIndex)
        assertEquals(ConstantPoolIndex(14), assertIs<ConstantMethodTypeEntry>(parsed[ConstantPoolIndex(19)]).descriptorIndex)
        assertEquals(BootstrapMethodIndex(0), assertIs<ConstantDynamicEntry>(parsed[ConstantPoolIndex(20)]).bootstrapMethodIndex)
        assertEquals(BootstrapMethodIndex(1), assertIs<ConstantInvokeDynamicEntry>(parsed[ConstantPoolIndex(21)]).bootstrapMethodIndex)
        assertEquals(ConstantPoolIndex(22), assertIs<ConstantModuleEntry>(parsed[ConstantPoolIndex(23)]).nameIndex)
        assertEquals(ConstantPoolIndex(24), assertIs<ConstantPackageEntry>(parsed[ConstantPoolIndex(25)]).nameIndex)
    }

    @Test
    fun `preserves CONSTANT_Utf8 modified UTF-8 payload bytes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("\u0000", byteArrayOf(0xC0.toByte(), 0x80.toByte())),
            ),
        )

        val bytes = ClassFileWriter.writeConstantPool(constantPool)

        assertContentEquals(
            byteArrayOf(0, 2, 1, 0, 2, 0xC0.toByte(), 0x80.toByte()),
            bytes,
        )

        val parsed = assertIs<ConstantUtf8Entry>(
            ConstantPoolParser.parse(ClassFileByteReader(bytes, source = "written-utf8.class"))[ConstantPoolIndex(1)],
        )
        assertEquals("\u0000", parsed.value)
        assertContentEquals(byteArrayOf(0xC0.toByte(), 0x80.toByte()), parsed.encodedBytes)
    }

    private fun utf8(value: String): ConstantUtf8Entry =
        ConstantUtf8Entry(value, value.encodeToByteArray())
}
