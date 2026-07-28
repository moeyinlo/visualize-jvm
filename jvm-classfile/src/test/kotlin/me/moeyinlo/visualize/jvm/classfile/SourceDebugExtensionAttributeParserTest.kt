package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SourceDebugExtensionAttributeParserTest {
    @Test
    fun `parses SourceDebugExtension UTF-8 text and preserves bytes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("SourceDebugExtension", byteArrayOf()),
            ),
        )
        val debugBytes = "SMAP\nMain.kt\nKotlin\n".encodeToByteArray()
        val readerBytes = byteArrayOf(0, 1, 0, 1, 0, 0, 0, debugBytes.size.toByte()) + debugBytes

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(readerBytes, source = "source-debug.class"),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("SourceDebugExtension" to SourceDebugExtensionAttributeParser),
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<SourceDebugExtensionAttribute>(attributes.single())
        assertEquals("SMAP\nMain.kt\nKotlin\n", attribute.text)
        assertContentEquals(debugBytes, attribute.debugExtension)

        attribute.debugExtension[0] = 0

        assertContentEquals(debugBytes, attribute.debugExtension)
    }

    @Test
    fun `rejects invalid SourceDebugExtension UTF-8 bytes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("SourceDebugExtension", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 1, 0x80.toByte()),
                    source = "bad-source-debug.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("SourceDebugExtension" to SourceDebugExtensionAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("SourceDebugExtension"), failure.message)
        assertTrue(failure.message.orEmpty().contains("UTF-8"), failure.message)
    }

    @Test
    fun `rejects SourceDebugExtension attributes before Java 5`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("SourceDebugExtension", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 1, 'x'.code.toByte()),
                    source = "java4-source-debug.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("SourceDebugExtension" to SourceDebugExtensionAttributeParser),
                ownerPath = "ClassFile",
                majorVersion = 48,
            )
        }

        assertTrue(failure.message.orEmpty().contains("SourceDebugExtension"), failure.message)
        assertTrue(failure.message.orEmpty().contains("major_version=48"), failure.message)
        assertTrue(failure.message.orEmpty().contains("49"), failure.message)
    }
}
