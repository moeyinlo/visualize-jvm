package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClassIdentityParserTest {
    @Test
    fun `parses this class super class and interfaces`() {
        val reader = ClassFileByteReader(
            byteArrayOf(
                0,
                2,
                0,
                3,
                0,
                2,
                0,
                4,
                0,
                5,
            ),
            source = "identity.class",
        )

        val identity = ClassIdentityParser.parse(reader)

        assertEquals(ConstantPoolIndex(2), identity.thisClassIndex)
        assertEquals(ConstantPoolIndex(3), identity.superClassIndex)
        assertEquals(listOf(ConstantPoolIndex(4), ConstantPoolIndex(5)), identity.interfaceIndexes)
        assertEquals(10, reader.position)
    }

    @Test
    fun `parses zero super class as no superclass`() {
        val reader = ClassFileByteReader(
            byteArrayOf(0, 1, 0, 0, 0, 0),
            source = "java-lang-Object.class",
        )

        val identity = ClassIdentityParser.parse(reader)

        assertEquals(ConstantPoolIndex(1), identity.thisClassIndex)
        assertNull(identity.superClassIndex)
        assertEquals(emptyList(), identity.interfaceIndexes)
    }

    @Test
    fun `rejects zero this class index`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassIdentityParser.parse(
                ClassFileByteReader(byteArrayOf(0, 0, 0, 0, 0, 0), source = "bad-this.class"),
            )
        }

        assertTrue(failure.message.orEmpty().contains("this_class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
    }

    @Test
    fun `rejects zero interface indexes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassIdentityParser.parse(
                ClassFileByteReader(byteArrayOf(0, 1, 0, 0, 0, 1, 0, 0), source = "bad-interface.class"),
            )
        }

        assertTrue(failure.message.orEmpty().contains("interfaces[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
    }
}
