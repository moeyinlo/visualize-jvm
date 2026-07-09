package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AttributeParserRegistryTest {
    @Test
    fun `dispatches attributes by UTF-8 constant pool name`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ExampleAttribute", byteArrayOf()),
            ),
        )
        val registry = AttributeParserRegistry.of(
            "ExampleAttribute" to AttributeBodyParser { context ->
                ParsedTestAttribute(
                    nameIndex = context.nameIndex,
                    name = context.name,
                    payload = context.reader.readSlice(context.reader.remaining),
                    rawInfo = context.info,
                )
            },
        )
        val reader = ClassFileByteReader(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                3,
                1,
                2,
                3,
            ),
            source = "attributes.class",
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = reader,
            constantPool = constantPool,
            registry = registry,
            ownerPath = "fields[0]",
        )

        val attribute = assertIs<ParsedTestAttribute>(attributes.single())
        assertEquals(ConstantPoolIndex(1), attribute.nameIndex)
        assertEquals("ExampleAttribute", attribute.name)
        assertContentEquals(byteArrayOf(1, 2, 3), attribute.payload)
        assertContentEquals(byteArrayOf(1, 2, 3), attribute.rawInfo)
        assertEquals(11, reader.position)
    }

    @Test
    fun `rejects attribute name index that is not a UTF-8 constant`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantIntegerEntry(7),
            ),
        )
        val reader = ClassFileByteReader(
            byteArrayOf(
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                0,
            ),
            source = "bad-attribute.class",
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = reader,
                constantPool = constantPool,
                registry = AttributeParserRegistry.Empty,
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0].attributes[0].attribute_name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
    }

    @Test
    fun `rejects parser that leaves bytes unread inside attribute info`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("PartialAttribute", byteArrayOf()),
            ),
        )
        val registry = AttributeParserRegistry.of(
            "PartialAttribute" to AttributeBodyParser { context ->
                context.reader.readU1()
                ParsedTestAttribute(
                    nameIndex = context.nameIndex,
                    name = context.name,
                    payload = byteArrayOf(),
                    rawInfo = context.info,
                )
            },
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 10, 11),
                    source = "partial-attribute.class",
                ),
                constantPool = constantPool,
                registry = registry,
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("PartialAttribute"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unconsumed"), failure.message)
    }

    @Test
    fun `reports source path and byte offset for parser format failures inside attribute info`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RejectingAttribute", byteArrayOf()),
            ),
        )
        val registry = AttributeParserRegistry.of(
            "RejectingAttribute" to AttributeBodyParser { context ->
                context.reader.readU1()
                throw ClassFileFormatException("parser rejected payload")
            },
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 10, 11),
                    source = "rejecting-attribute.class",
                ),
                constantPool = constantPool,
                registry = registry,
                ownerPath = "ClassFile",
            )
        }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("source=rejecting-attribute.class"), message)
        assertTrue(message.contains("path=ClassFile.attributes[0]"), message)
        assertTrue(message.contains("offset=9"), message)
        assertTrue(message.contains("parser rejected payload"), message)
    }

    @Test
    fun `preserves unregistered attributes as unknown attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("VendorSpecific", byteArrayOf()),
            ),
        )
        val reader = ClassFileByteReader(
            byteArrayOf(0, 1, 0, 1, 0, 0, 0, 4, 5, 6, 7, 8),
            source = "unknown-attribute.class",
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = reader,
            constantPool = constantPool,
            registry = AttributeParserRegistry.Empty,
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<UnknownAttributeInfo>(attributes.single())
        assertEquals(ConstantPoolIndex(1), attribute.nameIndex)
        assertEquals("VendorSpecific", attribute.name)
        assertContentEquals(byteArrayOf(5, 6, 7, 8), attribute.info)

        attribute.info[0] = 99

        assertContentEquals(byteArrayOf(5, 6, 7, 8), attribute.info)
    }

    private data class ParsedTestAttribute(
        override val nameIndex: ConstantPoolIndex,
        val name: String,
        val payload: ByteArray,
        val rawInfo: ByteArray,
    ) : AttributeInfo
}
