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
                utf8("()V"),
                ConstantMethodTypeEntry(ConstantPoolIndex(7)),
                ConstantMethodHandleEntry(MethodHandleReferenceKind.GetField, ConstantPoolIndex(6)),
                ConstantStringEntry(ConstantPoolIndex(3)),
                ConstantModuleEntry(ConstantPoolIndex(1)),
                ConstantPackageEntry(ConstantPoolIndex(1)),
                ConstantDynamicEntry(BootstrapMethodIndex(0), ConstantPoolIndex(5)),
                utf8("call"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(14), ConstantPoolIndex(7)),
                ConstantInvokeDynamicEntry(BootstrapMethodIndex(0), ConstantPoolIndex(15)),
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

    @Test
    fun `rejects class names that are not binary names in internal form`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("java.lang.Object"),
                ConstantClassEntry(ConstantPoolIndex(1)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("#2"), failure.message)
        assertTrue(failure.message.orEmpty().contains("name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("internal form"), failure.message)
    }

    @Test
    fun `rejects class names with empty internal name segments`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("java//Object"),
                ConstantClassEntry(ConstantPoolIndex(1)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("empty"), failure.message)
    }

    @Test
    fun `accepts array class names in constant class entries`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("[[I"),
                ConstantClassEntry(ConstantPoolIndex(1)),
                utf8("[Ljava/lang/String;"),
                ConstantClassEntry(ConstantPoolIndex(3)),
            ),
        )

        pool.validateReferences()
    }

    @Test
    fun `rejects invalid array class names in constant class entries`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("[V"),
                ConstantClassEntry(ConstantPoolIndex(1)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("#2"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field descriptor"), failure.message)
    }

    @Test
    fun `rejects empty unqualified names`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8(""),
                utf8("I"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(1), ConstantPoolIndex(2)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("#3"), failure.message)
        assertTrue(failure.message.orEmpty().contains("name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unqualified name"), failure.message)
    }

    @Test
    fun `rejects unqualified names with forbidden characters`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("bad/name"),
                utf8("I"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(1), ConstantPoolIndex(2)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("forbidden character '/'"), failure.message)
    }

    @Test
    fun `rejects invalid field descriptors on field references`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("Owner"),
                ConstantClassEntry(ConstantPoolIndex(1)),
                utf8("field"),
                utf8("V"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(4)),
                ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(5)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("#6"), failure.message)
        assertTrue(failure.message.orEmpty().contains("descriptor_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field descriptor"), failure.message)
    }

    @Test
    fun `rejects invalid class names inside field descriptors`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("Owner"),
                ConstantClassEntry(ConstantPoolIndex(1)),
                utf8("field"),
                utf8("Ljava.lang.Object;"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(4)),
                ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(5)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("internal form"), failure.message)
    }

    @Test
    fun `rejects invalid method descriptors on method references`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("Owner"),
                ConstantClassEntry(ConstantPoolIndex(1)),
                utf8("method"),
                utf8("I"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(4)),
                ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(5)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("#6"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method descriptor"), failure.message)
    }

    @Test
    fun `accepts instance initialization method references returning void`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("Owner"),
                ConstantClassEntry(ConstantPoolIndex(1)),
                utf8("<init>"),
                utf8("()V"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(4)),
                ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(5)),
            ),
        )

        pool.validateReferences()
    }

    @Test
    fun `rejects instance initialization method references returning a value`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("Owner"),
                ConstantClassEntry(ConstantPoolIndex(1)),
                utf8("<init>"),
                utf8("()I"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(4)),
                ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(5)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("<init>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("void"), failure.message)
    }

    @Test
    fun `rejects class initialization method references`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("Owner"),
                ConstantClassEntry(ConstantPoolIndex(1)),
                utf8("<clinit>"),
                utf8("()V"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(4)),
                ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(5)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("<clinit>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method name"), failure.message)
    }

    @Test
    fun `rejects non special method references containing angle brackets`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("Owner"),
                ConstantClassEntry(ConstantPoolIndex(1)),
                utf8("bad<name>"),
                utf8("()V"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(4)),
                ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(5)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("forbidden character '<'"), failure.message)
    }

    @Test
    fun `rejects interface method references to instance initialization methods`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("Owner"),
                ConstantClassEntry(ConstantPoolIndex(1)),
                utf8("<init>"),
                utf8("()V"),
                ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(4)),
                ConstantInterfaceMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(5)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("<init>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("not permitted"), failure.message)
    }

    @Test
    fun `rejects invalid method descriptors on method type constants`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("(V)V"),
                ConstantMethodTypeEntry(ConstantPoolIndex(1)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("#2"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method descriptor"), failure.message)
    }

    @Test
    fun `rejects invalid module names`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("bad@module"),
                ConstantModuleEntry(ConstantPoolIndex(1)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("#2"), failure.message)
        assertTrue(failure.message.orEmpty().contains("module name"), failure.message)
    }

    @Test
    fun `accepts escaped module name reserved characters`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("good\\@module"),
                ConstantModuleEntry(ConstantPoolIndex(1)),
            ),
        )

        pool.validateReferences()
    }

    @Test
    fun `rejects package names that are not in internal form`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                utf8("bad.package"),
                ConstantPackageEntry(ConstantPoolIndex(1)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            pool.validateReferences()
        }

        assertTrue(failure.message.orEmpty().contains("#2"), failure.message)
        assertTrue(failure.message.orEmpty().contains("internal form"), failure.message)
    }

    private fun utf8(value: String): ConstantUtf8Entry =
        ConstantUtf8Entry(value, value.encodeToByteArray())
}
