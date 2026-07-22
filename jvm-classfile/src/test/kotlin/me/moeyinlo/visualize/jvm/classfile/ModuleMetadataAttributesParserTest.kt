package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ModuleMetadataAttributesParserTest {
    @Test
    fun `parses ModulePackages attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ModulePackages", byteArrayOf()),
                ConstantUtf8Entry("pkg/one", byteArrayOf()),
                ConstantPackageEntry(ConstantPoolIndex(2)),
                ConstantUtf8Entry("pkg/two", byteArrayOf()),
                ConstantPackageEntry(ConstantPoolIndex(4)),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 6, 0, 2, 0, 3, 0, 5),
                source = "module-packages.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("ModulePackages" to ModulePackagesAttributeParser),
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<ModulePackagesAttribute>(attributes.single())
        assertEquals(listOf(ConstantPoolIndex(3), ConstantPoolIndex(5)), attribute.packageIndexes)
    }

    @Test
    fun `parses ModuleMainClass attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ModuleMainClass", byteArrayOf()),
                ConstantUtf8Entry("app/Main", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(2)),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 3),
                source = "module-main-class.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("ModuleMainClass" to ModuleMainClassAttributeParser),
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<ModuleMainClassAttribute>(attributes.single())
        assertEquals(ConstantPoolIndex(3), attribute.mainClassIndex)
    }

    @Test
    fun `rejects ModulePackages entry that is not a package constant`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ModulePackages", byteArrayOf()),
                ConstantUtf8Entry("not/a/package", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 4, 0, 1, 0, 2),
                    source = "bad-module-packages.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("ModulePackages" to ModulePackagesAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("package_index[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Package"), failure.message)
    }

    @Test
    fun `rejects duplicate ModulePackages package indexes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ModulePackages", byteArrayOf()),
                ConstantUtf8Entry("pkg/one", byteArrayOf()),
                ConstantPackageEntry(ConstantPoolIndex(2)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 6, 0, 2, 0, 3, 0, 3),
                    source = "bad-module-packages.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("ModulePackages" to ModulePackagesAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ClassFile.attributes[0].packages"), failure.message)
        assertTrue(failure.message.orEmpty().contains("duplicate package_index #3"), failure.message)
    }

    @Test
    fun `rejects ModuleMainClass attribute with non class index`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ModuleMainClass", byteArrayOf()),
                ConstantUtf8Entry("app/Main", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                    source = "bad-module-main-class.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("ModuleMainClass" to ModuleMainClassAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("main_class_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Class"), failure.message)
    }
}
