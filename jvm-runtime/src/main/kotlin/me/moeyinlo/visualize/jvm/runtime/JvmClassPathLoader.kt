package me.moeyinlo.visualize.jvm.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import me.moeyinlo.visualize.jvm.classfile.ClassFileFormatException
import me.moeyinlo.visualize.jvm.classfile.ClassFileParser
import me.moeyinlo.visualize.jvm.classfile.ClassFileReadException

sealed interface JvmClassPathEntry {
    data class Directory(val root: Path) : JvmClassPathEntry

    data class Jar(val jarPath: Path) : JvmClassPathEntry
}

class JvmClassPathLoader(
    private val entries: List<JvmClassPathEntry>,
    private val methodArea: JvmMethodArea,
    private val definingLoader: JvmClassLoaderIdentity = JvmClassLoaderIdentity.Bootstrap,
    private val initiatingLoader: JvmClassLoaderIdentity = definingLoader,
    private val loadingConstraints: JvmLoadingConstraintSet? = null,
) {
    fun load(internalName: String): JvmMethodAreaEntry {
        require(internalName.isNotBlank()) { "class internal name must not be blank" }

        loadingConstraints
            ?.resolvedClass(internalName, initiatingLoader)
            ?.let(methodArea::getClass)
            ?.let { entry ->
                return methodArea.recordInitiatingLoader(entry.loadedClassKey!!, initiatingLoader)
            }

        val classBytes = entries.firstNotNullOfOrNull { entry -> entry.findClassBytes(internalName) }
            ?: throw JvmClassPathLookupException(internalName)
        val methodAreaEntry = try {
            ClassFileParser.parse(bytes = classBytes.bytes, source = classBytes.source)
                .toJvmMethodAreaEntry()
        } catch (exception: ClassFileFormatException) {
            throw JvmClassPathFormatException(
                internalName = internalName,
                source = classBytes.source,
                cause = exception,
            )
        } catch (exception: ClassFileReadException) {
            throw JvmClassPathFormatException(
                internalName = internalName,
                source = classBytes.source,
                cause = exception,
            )
        }
        val definedInternalName = methodAreaEntry.definition.internalName
        if (definedInternalName != internalName) {
            throw JvmClassPathNameMismatchException(
                "Class path entry for $internalName defined $definedInternalName instead",
            )
        }
        val keyedEntry = methodAreaEntry.copy(
            loadedClassKey = JvmLoadedClassKey(
                internalName = definedInternalName,
                definingLoader = definingLoader,
            ),
            initiatingLoaders = setOf(definingLoader, initiatingLoader),
        )
        loadingConstraints?.recordResolution(
            internalName = definedInternalName,
            initiatingLoader = initiatingLoader,
            resolvedClass = keyedEntry.loadedClassKey!!,
        )
        try {
            methodArea.defineClass(keyedEntry)
        } catch (exception: JvmMethodAreaDefinitionException) {
            throw JvmClassPathDuplicateDefinitionException(
                loadedClassKey = keyedEntry.loadedClassKey!!,
                cause = exception,
            )
        }
        return keyedEntry
    }

    private fun JvmClassPathEntry.findClassBytes(internalName: String): ClassPathClassBytes? {
        val entryName = "$internalName.class"
        return when (this) {
            is JvmClassPathEntry.Directory -> {
                require(Files.isDirectory(root)) { "classpath root must be a directory: $root" }
                val classFilePath = root.resolve(entryName)
                if (Files.isRegularFile(classFilePath)) {
                    ClassPathClassBytes(Files.readAllBytes(classFilePath), classFilePath.toString())
                } else {
                    null
                }
            }
            is JvmClassPathEntry.Jar -> {
                require(Files.isRegularFile(jarPath)) { "jar classpath entry must be a regular file: $jarPath" }
                JarFile(jarPath.toFile()).use { jar ->
                    val jarEntry = jar.getJarEntry(entryName) ?: return null
                    ClassPathClassBytes(
                        bytes = jar.getInputStream(jarEntry).use { input -> input.readBytes() },
                        source = "$jarPath!/$entryName",
                    )
                }
            }
        }
    }
}

private data class ClassPathClassBytes(
    val bytes: ByteArray,
    val source: String,
)

class JvmClassPathLookupException(
    val internalName: String,
) : IllegalStateException("Class $internalName is not present on the classpath") {
    val guestThrowableClassName: String = "java/lang/NoClassDefFoundError"
}

class JvmClassPathFormatException(
    val internalName: String,
    val source: String,
    cause: Throwable,
) : IllegalStateException("Class $internalName has malformed classfile bytes from $source", cause) {
    val guestThrowableClassName: String = "java/lang/ClassFormatError"
}

class JvmClassPathDuplicateDefinitionException(
    val loadedClassKey: JvmLoadedClassKey,
    cause: Throwable,
) : IllegalStateException("Class ${loadedClassKey.diagnosticName} is already defined by the classpath loader", cause) {
    val internalName: String = loadedClassKey.internalName
    val guestThrowableClassName: String = "java/lang/LinkageError"
}

class JvmClassPathNameMismatchException(message: String) : IllegalStateException(message)
