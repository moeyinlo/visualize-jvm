package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MethodInfoParserTest {
    @Test
    fun `parses method declarations including raw attributes`() {
        val reader = ClassFileByteReader(
            byteArrayOf(
                0,
                2,
                0,
                0x01,
                0,
                3,
                0,
                4,
                0,
                0,
                0,
                0x09,
                0,
                5,
                0,
                6,
                0,
                1,
                0,
                7,
                0,
                0,
                0,
                2,
                20,
                21,
            ),
            source = "methods.class",
        )

        val methods = MethodInfoParser.parseMethods(reader)

        assertEquals(2, methods.size)
        assertEquals(0x0001, methods[0].accessFlags)
        assertEquals(ConstantPoolIndex(3), methods[0].nameIndex)
        assertEquals(ConstantPoolIndex(4), methods[0].descriptorIndex)
        assertEquals(emptyList(), methods[0].attributes)

        assertEquals(0x0009, methods[1].accessFlags)
        assertEquals(ConstantPoolIndex(5), methods[1].nameIndex)
        assertEquals(ConstantPoolIndex(6), methods[1].descriptorIndex)
        val attribute = assertIs<RawAttributeInfo>(methods[1].attributes.single())
        assertEquals(ConstantPoolIndex(7), attribute.nameIndex)
        assertContentEquals(byteArrayOf(20, 21), attribute.info)
        assertEquals(26, reader.position)
    }

    @Test
    fun `rejects zero method descriptor index`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 2, 0, 0, 0, 0),
                    source = "bad-method.class",
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0].descriptor_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
    }

    @Test
    fun `validates method names and descriptors when constant pool is available`() {
        val methods = parseValidatedMethods(
            methodName = "run",
            descriptor = "()V",
            accessFlags = 0x0001,
        )

        assertEquals(ConstantPoolIndex(1), methods.single().nameIndex)
        assertEquals(ConstantPoolIndex(2), methods.single().descriptorIndex)
    }

    @Test
    fun `rejects non special method names containing angle brackets`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "bad<name>", descriptor = "()V")
        }

        assertTrue(failure.message.orEmpty().contains("methods[0].name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method name"), failure.message)
    }

    @Test
    fun `rejects duplicate method name and descriptor pairs`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(accessFlags = 0x0001),
                        methodEntry(accessFlags = 0x0002),
                    ),
                    source = "bad-method.class",
                ),
                constantPool = methodValidationPool("run", "()V"),
                attributeParsers = AttributeParserRegistry.Empty,
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("Duplicate method_info"), failure.message)
        assertTrue(failure.message.orEmpty().contains("methods[1]"), failure.message)
    }

    @Test
    fun `rejects instance initialization methods in interfaces`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(
                methodName = "<init>",
                descriptor = "()V",
                accessFlags = 0x0001,
                classKind = ClassFileKind.Interface,
            )
        }

        assertTrue(failure.message.orEmpty().contains("<init>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("not permitted"), failure.message)
    }

    @Test
    fun `rejects instance initialization methods returning values`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "<init>", descriptor = "()I", accessFlags = 0x0001)
        }

        assertTrue(failure.message.orEmpty().contains("<init>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("void"), failure.message)
    }

    @Test
    fun `accepts unassigned strict bit on modern instance initialization methods`() {
        val methods = parseValidatedMethods(methodName = "<init>", descriptor = "()V", accessFlags = 0x0801)

        assertEquals(0x0801, methods.single().accessFlags)
    }

    @Test
    fun `rejects class initialization methods with parameters on modern class files`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "<clinit>", descriptor = "(I)V", accessFlags = 0x0008)
        }

        assertTrue(failure.message.orEmpty().contains("<clinit>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("()V"), failure.message)
    }

    @Test
    fun `rejects class initialization methods without static flag`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "<clinit>", descriptor = "()V", accessFlags = 0x0000)
        }

        assertTrue(failure.message.orEmpty().contains("<clinit>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_STATIC"), failure.message)
    }

    @Test
    fun `rejects class methods with multiple access visibility flags`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "run", descriptor = "()V", accessFlags = 0x0003)
        }

        assertTrue(failure.message.orEmpty().contains("ACC_PUBLIC"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_PRIVATE"), failure.message)
    }

    @Test
    fun `rejects abstract methods with static flags`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "run", descriptor = "()V", accessFlags = 0x0408)
        }

        assertTrue(failure.message.orEmpty().contains("ACC_ABSTRACT"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_STATIC"), failure.message)
    }

    @Test
    fun `rejects interface methods with protected flags`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(
                methodName = "run",
                descriptor = "()V",
                accessFlags = 0x0004,
                classKind = ClassFileKind.Interface,
            )
        }

        assertTrue(failure.message.orEmpty().contains("interface methods"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_PROTECTED"), failure.message)
    }

    @Test
    fun `rejects modern interface methods without exactly one of public or private`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(
                methodName = "run",
                descriptor = "()V",
                accessFlags = 0x0400,
                classKind = ClassFileKind.Interface,
            )
        }

        assertTrue(failure.message.orEmpty().contains("exactly one of ACC_PUBLIC and ACC_PRIVATE"), failure.message)
    }

    private fun parseValidatedMethods(
        methodName: String,
        descriptor: String,
        accessFlags: Int = 0x0001,
        classKind: ClassFileKind = ClassFileKind.Class,
        majorVersion: Int = 70,
    ): List<MethodInfo> =
        MethodInfoParser.parseMethods(
            reader = ClassFileByteReader(
                methodTable(methodEntry(accessFlags = accessFlags)),
                source = "validated-methods.class",
            ),
            constantPool = methodValidationPool(methodName, descriptor),
            attributeParsers = AttributeParserRegistry.Empty,
            classKind = classKind,
            majorVersion = majorVersion,
        )

    private fun methodValidationPool(
        name: String,
        descriptor: String,
    ): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry(name, name.encodeToByteArray()),
                ConstantUtf8Entry(descriptor, descriptor.encodeToByteArray()),
            ),
        )

    private fun methodTable(vararg methods: ByteArray): ByteArray =
        byteArrayOf(0, methods.size.toByte()) + methods.fold(byteArrayOf()) { bytes, method -> bytes + method }

    private fun methodEntry(
        accessFlags: Int,
        nameIndex: Int = 1,
        descriptorIndex: Int = 2,
    ): ByteArray =
        byteArrayOf(
            (accessFlags ushr 8).toByte(),
            accessFlags.toByte(),
            (nameIndex ushr 8).toByte(),
            nameIndex.toByte(),
            (descriptorIndex ushr 8).toByte(),
            descriptorIndex.toByte(),
            0,
            0,
        )
}
