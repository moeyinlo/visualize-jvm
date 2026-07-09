package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BootstrapMethodsAttributeParserTest {
    @Test
    fun `parses BootstrapMethods attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("BootstrapMethods", byteArrayOf()),
                ConstantUtf8Entry("java/lang/Object", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(2)),
                ConstantUtf8Entry("()V", byteArrayOf()),
                ConstantMethodTypeEntry(ConstantPoolIndex(4)),
                ConstantMethodHandleEntry(MethodHandleReferenceKind.InvokeStatic, ConstantPoolIndex(3)),
                ConstantUtf8Entry("arg", byteArrayOf()),
                ConstantStringEntry(ConstantPoolIndex(7)),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 12, 0, 1, 0, 6, 0, 3, 0, 3, 0, 5, 0, 8),
                source = "bootstrap-methods.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("BootstrapMethods" to BootstrapMethodsAttributeParser),
            ownerPath = "ClassFile",
        )

        val method = assertIs<BootstrapMethodsAttribute>(attributes.single()).bootstrapMethods.single()
        assertEquals(ConstantPoolIndex(6), method.bootstrapMethodRef)
        assertEquals(listOf(ConstantPoolIndex(3), ConstantPoolIndex(5), ConstantPoolIndex(8)), method.bootstrapArguments)
    }

    @Test
    fun `rejects bootstrap method ref that is not a method handle`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("BootstrapMethods", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 6, 0, 1, 0, 2, 0, 0),
                    source = "bad-bootstrap-method-ref.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("BootstrapMethods" to BootstrapMethodsAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("bootstrap_method_ref"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_MethodHandle"), failure.message)
    }

    @Test
    fun `rejects bootstrap argument that is not loadable`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("BootstrapMethods", byteArrayOf()),
                ConstantUtf8Entry("name", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantNameAndTypeEntry(ConstantPoolIndex(2), ConstantPoolIndex(3)),
                ConstantMethodHandleEntry(MethodHandleReferenceKind.InvokeStatic, ConstantPoolIndex(4)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 8, 0, 1, 0, 5, 0, 1, 0, 4),
                    source = "bad-bootstrap-argument.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("BootstrapMethods" to BootstrapMethodsAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("bootstrap_arguments[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("loadable"), failure.message)
    }
}
