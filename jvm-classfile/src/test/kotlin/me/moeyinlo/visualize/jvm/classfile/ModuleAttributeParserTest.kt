package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ModuleAttributeParserTest {
    @Test
    fun `parses Module attribute`() {
        val constantPool = moduleConstantPool()

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                bytes(
                    0, 1,
                    0, 1,
                    0, 0, 0, 44,
                    0, 3, 0, 0, 0, 4,
                    0, 1, 0, 6, 0x80, 0x20, 0, 4,
                    0, 1, 0, 8, 0x10, 0, 0, 1, 0, 10,
                    0, 1, 0, 8, 0x80, 0, 0, 0,
                    0, 1, 0, 12,
                    0, 1, 0, 12, 0, 1, 0, 14,
                ),
                source = "module-info.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<ModuleAttribute>(attributes.single())
        assertEquals(ConstantPoolIndex(3), attribute.moduleNameIndex)
        assertEquals(0x0000, attribute.moduleFlags)
        assertEquals(ConstantPoolIndex(4), attribute.moduleVersionIndex)
        assertEquals(ModuleRequires(ConstantPoolIndex(6), 0x8020, ConstantPoolIndex(4)), attribute.requires.single())
        assertEquals(ModuleExports(ConstantPoolIndex(8), 0x1000, listOf(ConstantPoolIndex(10))), attribute.exports.single())
        assertEquals(ModuleOpens(ConstantPoolIndex(8), 0x8000, emptyList()), attribute.opens.single())
        assertEquals(listOf(ConstantPoolIndex(12)), attribute.uses)
        assertEquals(ModuleProvides(ConstantPoolIndex(12), listOf(ConstantPoolIndex(14))), attribute.provides.single())
    }

    @Test
    fun `rejects module name index that is not a module constant`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Module", byteArrayOf()),
                ConstantUtf8Entry("not.a.module", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(0, 1, 0, 1, 0, 0, 0, 6, 0, 2, 0, 0, 0, 0),
                    source = "bad-module.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("module_name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Module"), failure.message)
    }

    @Test
    fun `rejects unknown Module flags`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 6,
                        0, 3, 0, 1, 0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].module_flags=0x0001"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unknown flag bits 0x0001"), failure.message)
    }

    @Test
    fun `rejects unknown Module requires flags`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 22,
                        0, 3, 0, 0, 0, 0,
                        0, 1,
                        0, 6, 0, 1, 0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].requires[0].requires_flags=0x0001"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unknown flag bits 0x0001"), failure.message)
    }

    @Test
    fun `rejects duplicate Module requires indexes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 28,
                        0, 3, 0, 0, 0, 0,
                        0, 2,
                        0, 6, 0, 0, 0, 0,
                        0, 6, 0, 0, 0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].requires"), failure.message)
        assertTrue(failure.message.orEmpty().contains("duplicate requires_index #6"), failure.message)
    }

    @Test
    fun `rejects synthetic java base requires`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 22,
                        0, 3, 0, 0, 0, 0,
                        0, 1,
                        0, 6, 0x10, 0, 0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].requires[0].requires_flags"), failure.message)
        assertTrue(failure.message.orEmpty().contains("requires java.base must not set ACC_SYNTHETIC"), failure.message)
    }

    @Test
    fun `rejects static phase java base requires for modern classfiles`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 22,
                        0, 3, 0, 0, 0, 0,
                        0, 1,
                        0, 6, 0, 0x40, 0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
                majorVersion = 54,
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].requires[0].requires_flags"), failure.message)
        assertTrue(failure.message.orEmpty().contains("requires java.base must not set ACC_STATIC_PHASE"), failure.message)
    }

    @Test
    fun `rejects java base module with requires entries`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 22,
                        0, 6, 0, 0, 0, 0,
                        0, 1,
                        0, 10, 0, 0, 0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].requires_count=1"), failure.message)
        assertTrue(failure.message.orEmpty().contains("java.base module must not declare requires entries"), failure.message)
    }

    @Test
    fun `rejects open module with opens entries`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 28,
                        0, 3, 0, 0x20, 0, 0,
                        0, 1,
                        0, 6, 0, 0, 0, 0,
                        0, 0,
                        0, 1,
                        0, 8, 0, 0, 0, 0,
                        0, 0,
                        0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].opens_count=1"), failure.message)
        assertTrue(failure.message.orEmpty().contains("open modules must not declare opens entries"), failure.message)
    }

    @Test
    fun `rejects duplicate Module exports indexes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 28,
                        0, 3, 0, 0, 0, 0,
                        0, 0,
                        0, 2,
                        0, 8, 0, 0, 0, 0,
                        0, 8, 0, 0, 0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].exports"), failure.message)
        assertTrue(failure.message.orEmpty().contains("duplicate exports_index #8"), failure.message)
    }

    @Test
    fun `rejects duplicate Module opens indexes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 28,
                        0, 3, 0, 0, 0, 0,
                        0, 0,
                        0, 0,
                        0, 2,
                        0, 8, 0, 0, 0, 0,
                        0, 8, 0, 0, 0, 0,
                        0, 0,
                        0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].opens"), failure.message)
        assertTrue(failure.message.orEmpty().contains("duplicate opens_index #8"), failure.message)
    }

    @Test
    fun `rejects duplicate Module uses indexes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 20,
                        0, 3, 0, 0, 0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 2,
                        0, 12,
                        0, 12,
                        0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].uses"), failure.message)
        assertTrue(failure.message.orEmpty().contains("duplicate uses_index #12"), failure.message)
    }

    @Test
    fun `rejects Module provides entry without implementations`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 26,
                        0, 3, 0, 0, 0, 0,
                        0, 1,
                        0, 6, 0, 0, 0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 1,
                        0, 12, 0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].provides[0].provides_with_count=0"), failure.message)
        assertTrue(failure.message.orEmpty().contains("provides_with_count must be nonzero"), failure.message)
    }

    @Test
    fun `rejects duplicate Module provides indexes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 28,
                        0, 3, 0, 0, 0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 2,
                        0, 12, 0, 1, 0, 14,
                        0, 12, 0, 1, 0, 14,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].provides"), failure.message)
        assertTrue(failure.message.orEmpty().contains("duplicate provides_index #12"), failure.message)
    }

    @Test
    fun `rejects duplicate Module exports to indexes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 26,
                        0, 3, 0, 0, 0, 0,
                        0, 0,
                        0, 1,
                        0, 8, 0, 0, 0, 2, 0, 10, 0, 10,
                        0, 0,
                        0, 0,
                        0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].exports[0].exports_to"), failure.message)
        assertTrue(failure.message.orEmpty().contains("duplicate exports_to_index #10"), failure.message)
    }

    @Test
    fun `rejects duplicate Module opens to indexes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 26,
                        0, 3, 0, 0, 0, 0,
                        0, 0,
                        0, 0,
                        0, 1,
                        0, 8, 0, 0, 0, 2, 0, 10, 0, 10,
                        0, 0,
                        0, 0,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].opens[0].opens_to"), failure.message)
        assertTrue(failure.message.orEmpty().contains("duplicate opens_to_index #10"), failure.message)
    }

    @Test
    fun `rejects duplicate Module provides with indexes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    bytes(
                        0, 1,
                        0, 1,
                        0, 0, 0, 24,
                        0, 3, 0, 0, 0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 1,
                        0, 12, 0, 2, 0, 14, 0, 14,
                    ),
                    source = "bad-module.class",
                ),
                constantPool = moduleConstantPool(),
                registry = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].provides[0].provides_with"), failure.message)
        assertTrue(failure.message.orEmpty().contains("duplicate provides_with_index #14"), failure.message)
    }

    private fun moduleConstantPool(): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Module", byteArrayOf()),
                ConstantUtf8Entry("my.module", byteArrayOf()),
                ConstantModuleEntry(ConstantPoolIndex(2)),
                ConstantUtf8Entry("1.0", byteArrayOf()),
                ConstantUtf8Entry("java.base", byteArrayOf()),
                ConstantModuleEntry(ConstantPoolIndex(5)),
                ConstantUtf8Entry("pkg", byteArrayOf()),
                ConstantPackageEntry(ConstantPoolIndex(7)),
                ConstantUtf8Entry("friend", byteArrayOf()),
                ConstantModuleEntry(ConstantPoolIndex(9)),
                ConstantUtf8Entry("service/Api", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(11)),
                ConstantUtf8Entry("service/Impl", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(13)),
            ),
        )

    private fun bytes(vararg values: Int): ByteArray = values.map { it.toByte() }.toByteArray()
}
