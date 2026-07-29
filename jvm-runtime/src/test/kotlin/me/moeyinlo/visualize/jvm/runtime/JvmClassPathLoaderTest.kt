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
import me.moeyinlo.visualize.jvm.classfile.ClassFileFormatException
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

    @Test
    fun `records the defining loader identity on loaded classpath classes`() {
        val root = Files.createTempDirectory("visualize-jvm-classpath-defining-loader")
        writeDirectoryClass(
            root,
            "pkg/Example",
            ClassFileWriter.writeClassFile(classFile("pkg/Example", superclassName = "java/lang/Object")),
        )
        val methodArea = JvmMethodArea()
        val definingLoader = JvmClassLoaderIdentity.UserDefined(id = 7, displayName = "app")
        val loader = JvmClassPathLoader(
            entries = listOf(JvmClassPathEntry.Directory(root)),
            methodArea = methodArea,
            definingLoader = definingLoader,
        )

        val loaded = loader.load("pkg/Example")

        assertEquals(
            JvmLoadedClassKey(internalName = "pkg/Example", definingLoader = definingLoader),
            loaded.loadedClassKey,
        )
    }

    @Test
    fun `records the initiating loader for classpath loads`() {
        val root = Files.createTempDirectory("visualize-jvm-classpath-initiating-loader")
        writeDirectoryClass(
            root,
            "pkg/Example",
            ClassFileWriter.writeClassFile(classFile("pkg/Example", superclassName = "java/lang/Object")),
        )
        val methodArea = JvmMethodArea()
        val definingLoader = JvmClassLoaderIdentity.UserDefined(id = 7, displayName = "app")
        val initiatingLoader = JvmClassLoaderIdentity.UserDefined(id = 8, displayName = "child")
        val loader = JvmClassPathLoader(
            entries = listOf(JvmClassPathEntry.Directory(root)),
            methodArea = methodArea,
            definingLoader = definingLoader,
            initiatingLoader = initiatingLoader,
        )

        val loaded = loader.load("pkg/Example")

        assertEquals(setOf(definingLoader, initiatingLoader), loaded.initiatingLoaders)
        assertSame(loaded, methodArea.getClass("pkg/Example", initiatingLoader))
    }

    @Test
    fun `reports missing classpath classes as guest NoClassDefFoundError`() {
        val root = Files.createTempDirectory("visualize-jvm-classpath-missing")
        val loader = JvmClassPathLoader(
            entries = listOf(JvmClassPathEntry.Directory(root)),
            methodArea = JvmMethodArea(),
        )

        val exception = assertFailsWith<JvmClassPathLookupException> {
            loader.load("pkg/Missing")
        }

        assertEquals("java/lang/NoClassDefFoundError", exception.guestThrowableClassName)
        assertEquals("pkg/Missing", exception.internalName)
        assertEquals("Class pkg/Missing is not present on the classpath", exception.message)
    }

    @Test
    fun `reports malformed classpath bytes as guest ClassFormatError`() {
        val root = Files.createTempDirectory("visualize-jvm-classpath-format-error")
        val malformedBytes = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        writeDirectoryClass(root, "pkg/Broken", malformedBytes)
        val methodArea = JvmMethodArea()
        val loader = JvmClassPathLoader(
            entries = listOf(JvmClassPathEntry.Directory(root)),
            methodArea = methodArea,
        )

        val exception = assertFailsWith<JvmClassPathFormatException> {
            loader.load("pkg/Broken")
        }

        assertEquals("java/lang/ClassFormatError", exception.guestThrowableClassName)
        assertEquals("pkg/Broken", exception.internalName)
        assertEquals(root.resolve("pkg/Broken.class").toString(), exception.source)
        assertEquals(ClassFileFormatException::class, exception.cause!!::class)
        assertFalse(methodArea.hasClass("pkg/Broken"))
    }

    @Test
    fun `records loading constraint resolution for classpath loads`() {
        val root = Files.createTempDirectory("visualize-jvm-classpath-loading-constraints")
        writeDirectoryClass(
            root,
            "pkg/Example",
            ClassFileWriter.writeClassFile(classFile("pkg/Example", superclassName = "java/lang/Object")),
        )
        val methodArea = JvmMethodArea()
        val loadingConstraints = JvmLoadingConstraintSet()
        val definingLoader = JvmClassLoaderIdentity.UserDefined(id = 7, displayName = "app")
        val initiatingLoader = JvmClassLoaderIdentity.UserDefined(id = 8, displayName = "child")
        val loader = JvmClassPathLoader(
            entries = listOf(JvmClassPathEntry.Directory(root)),
            methodArea = methodArea,
            definingLoader = definingLoader,
            initiatingLoader = initiatingLoader,
            loadingConstraints = loadingConstraints,
        )

        val loaded = loader.load("pkg/Example")

        assertEquals(loaded.loadedClassKey, loadingConstraints.resolvedClass("pkg/Example", initiatingLoader))
    }

    @Test
    fun `rejects constrained classpath resolutions before defining the class`() {
        val root = Files.createTempDirectory("visualize-jvm-classpath-loading-constraint-conflict")
        writeDirectoryClass(
            root,
            "pkg/Example",
            ClassFileWriter.writeClassFile(classFile("pkg/Example", superclassName = "java/lang/Object")),
        )
        val methodArea = JvmMethodArea()
        val loadingConstraints = JvmLoadingConstraintSet()
        val appLoader = JvmClassLoaderIdentity.UserDefined(id = 7, displayName = "app")
        val pluginLoader = JvmClassLoaderIdentity.UserDefined(id = 8, displayName = "plugin")
        val childLoader = JvmClassLoaderIdentity.UserDefined(id = 9, displayName = "child")
        val pluginClass = JvmLoadedClassKey("pkg/Example", pluginLoader)
        loadingConstraints.addConstraint("pkg/Example", childLoader, pluginLoader)
        loadingConstraints.recordResolution("pkg/Example", pluginLoader, pluginClass)
        val loader = JvmClassPathLoader(
            entries = listOf(JvmClassPathEntry.Directory(root)),
            methodArea = methodArea,
            definingLoader = appLoader,
            initiatingLoader = childLoader,
            loadingConstraints = loadingConstraints,
        )

        val exception = assertFailsWith<JvmLoadingConstraintViolationException> {
            loader.load("pkg/Example")
        }

        assertEquals("java/lang/LinkageError", exception.guestThrowableClassName)
        assertEquals(pluginClass, exception.expectedClass)
        assertEquals(JvmLoadedClassKey("pkg/Example", appLoader), exception.actualClass)
        assertFalse(methodArea.hasClass(JvmLoadedClassKey("pkg/Example", appLoader)))
    }

    @Test
    fun `reuses previously resolved constrained classes for initiating loaders`() {
        val root = Files.createTempDirectory("visualize-jvm-classpath-loading-constraint-reuse")
        writeDirectoryClass(
            root,
            "pkg/Example",
            ClassFileWriter.writeClassFile(classFile("pkg/Example", superclassName = "app/Base")),
        )
        val methodArea = JvmMethodArea()
        val loadingConstraints = JvmLoadingConstraintSet()
        val appLoader = JvmClassLoaderIdentity.UserDefined(id = 7, displayName = "app")
        val pluginLoader = JvmClassLoaderIdentity.UserDefined(id = 8, displayName = "plugin")
        val childLoader = JvmClassLoaderIdentity.UserDefined(id = 9, displayName = "child")
        val pluginClass = JvmLoadedClassKey("pkg/Example", pluginLoader)
        val pluginEntry = JvmMethodAreaEntry(
            definition = JvmClassDefinition(internalName = "pkg/Example", superclassName = "plugin/Base"),
            loadedClassKey = pluginClass,
            initiatingLoaders = setOf(pluginLoader),
        )
        methodArea.defineClass(pluginEntry)
        loadingConstraints.addConstraint("pkg/Example", childLoader, pluginLoader)
        loadingConstraints.recordResolution("pkg/Example", pluginLoader, pluginClass)
        val loader = JvmClassPathLoader(
            entries = listOf(JvmClassPathEntry.Directory(root)),
            methodArea = methodArea,
            definingLoader = appLoader,
            initiatingLoader = childLoader,
            loadingConstraints = loadingConstraints,
        )

        val loaded = loader.load("pkg/Example")

        assertEquals(pluginClass, loaded.loadedClassKey)
        assertEquals(setOf(pluginLoader, childLoader), loaded.initiatingLoaders)
        assertSame(loaded, methodArea.getClass("pkg/Example", childLoader))
        assertEquals(1, methodArea.classCount)
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
