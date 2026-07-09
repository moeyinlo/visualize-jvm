package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClassAccessFlagsTest {
    @Test
    fun `parses class access flags and preserves reserved bits`() {
        val reader = ClassFileByteReader(
            byteArrayOf(0x50, 0x39),
            source = "flags.class",
        )

        val flags = ClassAccessFlagsParser.parse(reader)

        assertEquals(0x5039, flags.raw)
        assertEquals(ClassFileKind.Class, flags.kind)
        assertTrue(flags.has(ClassAccessFlag.Public))
        assertTrue(flags.has(ClassAccessFlag.Final))
        assertTrue(flags.has(ClassAccessFlag.Super))
        assertTrue(flags.has(ClassAccessFlag.Synthetic))
        assertTrue(flags.has(ClassAccessFlag.Enum))
        assertFalse(flags.has(ClassAccessFlag.Interface))
        assertEquals(0x0008, flags.reservedBits)
        assertEquals(2, reader.position)
    }

    @Test
    fun `parses annotation interfaces`() {
        val flags = ClassAccessFlagsParser.parse(
            ClassFileByteReader(byteArrayOf(0x26, 0x01), source = "annotation.class"),
        )

        assertEquals(ClassFileKind.AnnotationInterface, flags.kind)
        assertTrue(flags.has(ClassAccessFlag.Public))
        assertTrue(flags.has(ClassAccessFlag.Interface))
        assertTrue(flags.has(ClassAccessFlag.Abstract))
        assertTrue(flags.has(ClassAccessFlag.Annotation))
    }

    @Test
    fun `parses module access flags only when no other flags are set`() {
        val flags = ClassAccessFlagsParser.parse(
            ClassFileByteReader(byteArrayOf(0x80.toByte(), 0x00), source = "module-info.class"),
        )

        assertEquals(ClassFileKind.Module, flags.kind)
        assertTrue(flags.has(ClassAccessFlag.Module))
    }

    @Test
    fun `rejects module flag combined with any other flag`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassAccessFlagsParser.parse(
                ClassFileByteReader(byteArrayOf(0x80.toByte(), 0x01), source = "bad-module.class"),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_MODULE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("no other flag"), failure.message)
    }

    @Test
    fun `rejects interfaces that are not abstract`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassAccessFlagsParser.parse(
                ClassFileByteReader(byteArrayOf(0x02, 0x00), source = "bad-interface.class"),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_INTERFACE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_ABSTRACT"), failure.message)
    }

    @Test
    fun `rejects illegal interface flags`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassAccessFlagsParser.parse(
                ClassFileByteReader(byteArrayOf(0x06, 0x10), source = "bad-interface.class"),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_INTERFACE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_FINAL"), failure.message)
    }

    @Test
    fun `rejects annotation flag without interface flag`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassAccessFlagsParser.parse(
                ClassFileByteReader(byteArrayOf(0x20, 0x00), source = "bad-annotation.class"),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_ANNOTATION"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_INTERFACE"), failure.message)
    }

    @Test
    fun `rejects classes that are both final and abstract`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassAccessFlagsParser.parse(
                ClassFileByteReader(byteArrayOf(0x04, 0x10), source = "bad-class.class"),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_FINAL"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_ABSTRACT"), failure.message)
    }
}
