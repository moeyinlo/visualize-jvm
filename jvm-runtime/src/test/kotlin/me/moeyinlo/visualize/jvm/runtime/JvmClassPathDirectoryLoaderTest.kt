package me.moeyinlo.visualize.jvm.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import me.moeyinlo.visualize.jvm.classfile.ClassAccessFlags
import me.moeyinlo.visualize.jvm.classfile.ClassFile
import me.moeyinlo.visualize.jvm.classfile.ClassFileKind
import me.moeyinlo.visualize.jvm.classfile.ClassFileMagic
import me.moeyinlo.visualize.jvm.classfile.ClassFileVersion
import me.moeyinlo.visualize.jvm.classfile.ClassFileWriter
import me.moeyinlo.visualize.jvm.classfile.ClassIdentity
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry

class JvmClassPathDirectoryLoaderTest {
    @Test
    fun `loads a class from a classpath directory by internal name`() {
        val classpathRoot = Files.createTempDirectory("visualize-jvm-classpath-directory-test")
        val classFilePath = classpathRoot.resolve("pkg").resolve("Example.class")
        Files.createDirectories(classFilePath.parent)
        Files.write(classFilePath, ClassFileWriter.writeClassFile(classFile("pkg/Example")))
        val methodArea = JvmMethodArea()
        val loader = JvmClassPathDirectoryLoader(classpathRoot, methodArea)

        val loaded = loader.load("pkg/Example")

        assertEquals("pkg/Example", loaded.definition.internalName)
        assertSame(loaded, methodArea.getClass("pkg/Example"))
    }

    private fun classFile(internalName: String): ClassFile =
        ClassFile(
            magic = ClassFileMagic(offset = 0, value = 0xCAFEBABEL),
            version = ClassFileVersion(offset = 4, minor = 0, major = 70),
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry(internalName, internalName.encodeToByteArray()),
                    ConstantClassEntry(ConstantPoolIndex(1)),
                    ConstantUtf8Entry("java/lang/Object", "java/lang/Object".encodeToByteArray()),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                ),
            ),
            accessFlags = ClassAccessFlags(raw = 0x0021, kind = ClassFileKind.Class, reservedBits = 0),
            identity = ClassIdentity(
                thisClassIndex = ConstantPoolIndex(2),
                superClassIndex = ConstantPoolIndex(4),
                interfaceIndexes = emptyList(),
            ),
            fields = emptyList(),
            methods = emptyList(),
            attributes = emptyList(),
        )
}
