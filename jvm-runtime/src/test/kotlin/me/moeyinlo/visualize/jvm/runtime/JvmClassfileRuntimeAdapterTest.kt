package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ClassAccessFlags
import me.moeyinlo.visualize.jvm.classfile.ClassFile
import me.moeyinlo.visualize.jvm.classfile.ClassFileKind
import me.moeyinlo.visualize.jvm.classfile.ClassFileMagic
import me.moeyinlo.visualize.jvm.classfile.ClassFileVersion
import me.moeyinlo.visualize.jvm.classfile.ClassIdentity
import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.CodeExceptionHandler
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import me.moeyinlo.visualize.jvm.classfile.ConstantValueAttribute
import me.moeyinlo.visualize.jvm.classfile.FieldInfo
import me.moeyinlo.visualize.jvm.classfile.MethodInfo

class JvmClassfileRuntimeAdapterTest {
    @Test
    fun `classfile adapter maps class identity hierarchy fields and methods to runtime definition`() {
        val classFile = ClassFile(
            magic = ClassFileMagic(offset = 0, value = 0xCAFEBABEL),
            version = ClassFileVersion(offset = 4, minor = 0, major = 70),
            constantPool = constantPool(),
            accessFlags = ClassAccessFlags(raw = 0x0020, kind = ClassFileKind.Class, reservedBits = 0),
            identity = ClassIdentity(
                thisClassIndex = ConstantPoolIndex(2),
                superClassIndex = ConstantPoolIndex(4),
                interfaceIndexes = listOf(ConstantPoolIndex(6)),
            ),
            fields = listOf(
                FieldInfo(
                    accessFlags = 0x000A,
                    nameIndex = ConstantPoolIndex(7),
                    descriptorIndex = ConstantPoolIndex(8),
                    attributes = emptyList(),
                ),
                FieldInfo(
                    accessFlags = 0,
                    nameIndex = ConstantPoolIndex(9),
                    descriptorIndex = ConstantPoolIndex(10),
                    attributes = emptyList(),
                ),
            ),
            methods = listOf(
                MethodInfo(
                    accessFlags = 0x0001,
                    nameIndex = ConstantPoolIndex(11),
                    descriptorIndex = ConstantPoolIndex(12),
                    attributes = listOf(
                        CodeAttribute(
                            nameIndex = ConstantPoolIndex(13),
                            maxStack = 1,
                            maxLocals = 1,
                            code = byteArrayOf(0x04, 0xAC.toByte()),
                            exceptionTable = listOf(
                                CodeExceptionHandler(
                                    startPc = 0,
                                    endPc = 1,
                                    handlerPc = 1,
                                    catchType = ConstantPoolIndex(18),
                                ),
                                CodeExceptionHandler(
                                    startPc = 1,
                                    endPc = 2,
                                    handlerPc = 1,
                                    catchType = null,
                                ),
                            ),
                        ),
                    ),
                ),
                MethodInfo(
                    accessFlags = 0x0181,
                    nameIndex = ConstantPoolIndex(14),
                    descriptorIndex = ConstantPoolIndex(15),
                    attributes = emptyList(),
                ),
            ),
            attributes = emptyList(),
        )

        val definition = classFile.toJvmClassDefinition()

        assertEquals("pkg/Example", definition.internalName)
        assertEquals("java/lang/Object", definition.superclassName)
        assertEquals(listOf("pkg/Itf"), definition.interfaceNames)
        assertEquals(false, definition.isInterface)
        assertEquals(
            listOf(
                JvmFieldDefinition(
                    name = "count",
                    descriptor = "I",
                    isStatic = true,
                    isPrivate = true,
                    isPackagePrivate = false,
                ),
                JvmFieldDefinition(
                    name = "name",
                    descriptor = "Ljava/lang/String;",
                    isStatic = false,
                    isPackagePrivate = true,
                ),
            ),
            definition.fields,
        )
        assertEquals(2, definition.methods.size)
        val valueMethod = definition.methods[0]
        assertEquals("value", valueMethod.name)
        assertEquals("()I", valueMethod.descriptor)
        assertEquals(false, valueMethod.isStatic)
        assertEquals(1, valueMethod.maxStack)
        assertEquals(1, valueMethod.maxLocals)
        assertContentEquals(byteArrayOf(0x04, 0xAC.toByte()), valueMethod.code)
        assertEquals(
            listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 1,
                    handlerPc = 1,
                    catchClassName = "java/lang/RuntimeException",
                ),
                JvmExceptionHandler(
                    startPc = 1,
                    endPc = 2,
                    handlerPc = 1,
                    catchClassName = null,
                ),
            ),
            valueMethod.exceptionHandlers,
        )
        assertEquals(
            JvmMethodDefinition(
                name = "nativeVarargs",
                descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
                isStatic = false,
                isNative = true,
                isVarargs = true,
            ),
            definition.methods[1],
        )
    }

    @Test
    fun `classfile adapter creates method area entries`() {
        val classFile = ClassFile(
            magic = ClassFileMagic(offset = 0, value = 0xCAFEBABEL),
            version = ClassFileVersion(offset = 4, minor = 0, major = 70),
            constantPool = constantPool(),
            accessFlags = ClassAccessFlags(raw = 0x0020, kind = ClassFileKind.Class, reservedBits = 0),
            identity = ClassIdentity(
                thisClassIndex = ConstantPoolIndex(2),
                superClassIndex = ConstantPoolIndex(4),
                interfaceIndexes = emptyList(),
            ),
            fields = emptyList(),
            methods = emptyList(),
            attributes = emptyList(),
        )

        val methodArea = JvmMethodArea()
        methodArea.defineClass(classFile.toJvmMethodAreaEntry())

        assertEquals("pkg/Example", methodArea.getClass("pkg/Example").definition.internalName)
    }

    @Test
    fun `classfile adapter maps numeric ConstantValue field attributes to runtime metadata`() {
        val classFile = ClassFile(
            magic = ClassFileMagic(offset = 0, value = 0xCAFEBABEL),
            version = ClassFileVersion(offset = 4, minor = 0, major = 70),
            constantPool = constantPool(),
            accessFlags = ClassAccessFlags(raw = 0x0020, kind = ClassFileKind.Class, reservedBits = 0),
            identity = ClassIdentity(
                thisClassIndex = ConstantPoolIndex(2),
                superClassIndex = ConstantPoolIndex(4),
                interfaceIndexes = emptyList(),
            ),
            fields = listOf(
                FieldInfo(
                    accessFlags = 0x0008,
                    nameIndex = ConstantPoolIndex(7),
                    descriptorIndex = ConstantPoolIndex(8),
                    attributes = listOf(
                        ConstantValueAttribute(
                            nameIndex = ConstantPoolIndex(19),
                            constantValueIndex = ConstantPoolIndex(20),
                        ),
                    ),
                ),
            ),
            methods = emptyList(),
            attributes = emptyList(),
        )

        assertEquals(
            JvmFieldDefinition(
                name = "count",
                descriptor = "I",
                isStatic = true,
                isPackagePrivate = true,
                constantValue = JvmIntValue(42),
            ),
            classFile.toJvmClassDefinition().fields.single(),
        )
    }

    @Test
    fun `classfile adapter reports String ConstantValue requires guest string preparation`() {
        val classFile = ClassFile(
            magic = ClassFileMagic(offset = 0, value = 0xCAFEBABEL),
            version = ClassFileVersion(offset = 4, minor = 0, major = 70),
            constantPool = constantPool(),
            accessFlags = ClassAccessFlags(raw = 0x0020, kind = ClassFileKind.Class, reservedBits = 0),
            identity = ClassIdentity(
                thisClassIndex = ConstantPoolIndex(2),
                superClassIndex = ConstantPoolIndex(4),
                interfaceIndexes = emptyList(),
            ),
            fields = listOf(
                FieldInfo(
                    accessFlags = 0x0008,
                    nameIndex = ConstantPoolIndex(9),
                    descriptorIndex = ConstantPoolIndex(10),
                    attributes = listOf(
                        ConstantValueAttribute(
                            nameIndex = ConstantPoolIndex(19),
                            constantValueIndex = ConstantPoolIndex(22),
                        ),
                    ),
                ),
            ),
            methods = emptyList(),
            attributes = emptyList(),
        )

        val exception = assertFailsWith<JvmClassfileRuntimeAdapterException> {
            classFile.toJvmClassDefinition()
        }

        assertEquals(
            "String ConstantValue attributes require guest String heap preparation",
            exception.message,
        )
    }

    private fun constantPool(): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("pkg/Example", "pkg/Example".encodeToByteArray()),
                ConstantClassEntry(ConstantPoolIndex(1)),
                ConstantUtf8Entry("java/lang/Object", "java/lang/Object".encodeToByteArray()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("pkg/Itf", "pkg/Itf".encodeToByteArray()),
                ConstantClassEntry(ConstantPoolIndex(5)),
                ConstantUtf8Entry("count", "count".encodeToByteArray()),
                ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ConstantUtf8Entry("name", "name".encodeToByteArray()),
                ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                ConstantUtf8Entry("value", "value".encodeToByteArray()),
                ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ConstantUtf8Entry("Code", "Code".encodeToByteArray()),
                ConstantUtf8Entry("nativeVarargs", "nativeVarargs".encodeToByteArray()),
                ConstantUtf8Entry(
                    "([Ljava/lang/Object;)Ljava/lang/Object;",
                    "([Ljava/lang/Object;)Ljava/lang/Object;".encodeToByteArray(),
                ),
                ConstantNameAndTypeEntry(ConstantPoolIndex(11), ConstantPoolIndex(12)),
                ConstantUtf8Entry("java/lang/RuntimeException", "java/lang/RuntimeException".encodeToByteArray()),
                ConstantClassEntry(ConstantPoolIndex(17)),
                ConstantUtf8Entry("ConstantValue", "ConstantValue".encodeToByteArray()),
                ConstantIntegerEntry(42),
                ConstantUtf8Entry("literal", "literal".encodeToByteArray()),
                ConstantStringEntry(ConstantPoolIndex(21)),
            ),
        )
}
