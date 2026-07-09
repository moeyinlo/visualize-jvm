package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConstantPoolReferenceValidationTest {
    @Test
    fun `accepts constant pool entries whose references point to expected entry types`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("Owner"),
                ConstantClassEntry(ConstantPoolIndex(1)),
                utf8("field"),
                utf8("I"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(4)),
                ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(5)),
                ConstantMethodTypeEntry(ConstantPoolIndex(4)),
                ConstantMethodHandleEntry(MethodHandleReferenceKind.GetField, ConstantPoolIndex(6)),
                ConstantStringEntry(ConstantPoolIndex(3)),
                ConstantModuleEntry(ConstantPoolIndex(1)),
                ConstantPackageEntry(ConstantPoolIndex(1)),
                ConstantDynamicEntry(BootstrapMethodIndex(0), ConstantPoolIndex(5)),
                ConstantInvokeDynamicEntry(BootstrapMethodIndex(0), ConstantPoolIndex(5)),
            ),
        )

        pool.validateReferences()
    }

    @Test
    fun `rejects references to the wrong constant pool entry type`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                ConstantClassEntry(ConstantPoolIndex(2)),
                ConstantIntegerEntry(123),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("#1"), failure.message)
        assertTrue(failure.message.orEmpty().contains("name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("#2"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ConstantUtf8Entry"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ConstantIntegerEntry"), failure.message)
    }

    @Test
    fun `rejects references to unusable wide constant pool slots`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                ConstantLongEntry(1),
                ConstantClassEntry(ConstantPoolIndex(2)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("#3"), failure.message)
        assertTrue(failure.message.orEmpty().contains("name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("#2"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unusable"), failure.message)
    }

    private fun utf8(value: String): ConstantUtf8Entry =
        ConstantUtf8Entry(value, value.encodeToByteArray())
}
