package me.moeyinlo.visualize.jvm.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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

class JvmClassPathLoaderTest {
    @Test
    fun `loads the first matching classpath entry in order`() {
        val firstRoot = Files.createTempDirectory("visualize-jvm-classpath-first")
        val secondJar = Files.createTempDirectory("visualize-jvm-classpath-second").resolve("classes.jar")
        writeDirectoryClass(
            firstRoot,
            "pkg/Example",
            ClassFileWriter.writeClassFile(classFile("pkg/Example", superclassName = "java/lang/Object")),
        )
        writeJarClass(
            secondJar,
            "pkg/Example",
            ClassFileWriter.writeClassFile(classFile("pkg/Example", superclassName = "pkg/ShadowParent")),
        )
        val methodArea = JvmMethodArea()
        val loader = JvmClassPathLoader(
            entries = listOf(
                JvmClassPathEntry.Directory(firstRoot),
                JvmClassPathEntry.Jar(secondJar),
            ),
            methodArea = methodArea,
        )

        val loaded = loader.load("pkg/Example")

        assertEquals("java/lang/Object", loaded.definition.superclassName)
        assertSame(loaded, methodArea.getClass("pkg/Example"))
    }

    @Test
    fun `rejects classpath entries whose defined class name differs from the requested name`() {
        val root = Files.createTempDirectory("visualize-jvm-classpath-name-mismatch")
        writeDirectoryClass(
            root,
            "pkg/Requested",
            ClassFileWriter.writeClassFile(classFile("pkg/Actual", superclassName = "java/lang/Object")),
        )
        val methodArea = JvmMethodArea()
        val loader = JvmClassPathLoader(
            entries = listOf(JvmClassPathEntry.Directory(root)),
            methodArea = methodArea,
        )

        val exception = assertFailsWith<JvmClassPathNameMismatchException> {
            loader.load("pkg/Requested")
        }

        assertEquals(
            "Class path entry for pkg/Requested defined pkg/Actual instead",
            exception.message,
        )
        assertFalse(methodArea.hasClass("pkg/Requested"))
        assertFalse(methodArea.hasClass("pkg/Actual"))
    }

    private fun writeDirectoryClass(
        root: Path,
        internalName: String,
        bytes: ByteArray,
    ) {
        val classFilePath = root.resolve("$internalName.class")
        Files.createDirectories(classFilePath.parent)
        Files.write(classFilePath, bytes)
    }

    private fun writeJarClass(
        jarPath: Path,
        internalName: String,
        bytes: ByteArray,
    ) {
        JarOutputStream(Files.newOutputStream(jarPath)).use { jar ->
            jar.putNextEntry(JarEntry("$internalName.class"))
            jar.write(bytes)
            jar.closeEntry()
        }
    }

    private fun classFile(
        internalName: String,
        superclassName: String,
    ): ClassFile =
        ClassFile(
            magic = ClassFileMagic(offset = 0, value = 0xCAFEBABEL),
            version = ClassFileVersion(offset = 4, minor = 0, major = 70),
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry(internalName, internalName.encodeToByteArray()),
                    ConstantClassEntry(ConstantPoolIndex(1)),
                    ConstantUtf8Entry(superclassName, superclassName.encodeToByteArray()),
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
