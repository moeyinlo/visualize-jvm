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

class JvmClassFilePathLoaderTest {
    @Test
    fun `loads a single class file path into the method area`() {
        val tempDirectory = Files.createTempDirectory("visualize-jvm-classpath-test")
        val classFilePath = tempDirectory.resolve("Example.class")
        val classFile = ClassFile(
            magic = ClassFileMagic(offset = 0, value = 0xCAFEBABEL),
            version = ClassFileVersion(offset = 4, minor = 0, major = 70),
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("pkg/Example", "pkg/Example".encodeToByteArray()),
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
        Files.write(classFilePath, ClassFileWriter.writeClassFile(classFile))
        val methodArea = JvmMethodArea()
        val loader = JvmClassFilePathLoader(methodArea)

        val loaded = loader.load(classFilePath)

        assertEquals("pkg/Example", loaded.definition.internalName)
        assertSame(loaded, methodArea.getClass("pkg/Example"))
    }
}
