package me.moeyinlo.visualize.jvm.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import me.moeyinlo.visualize.jvm.classfile.ClassFileParser

sealed interface JvmClassPathEntry {
    data class Directory(val root: Path) : JvmClassPathEntry

    data class Jar(val jarPath: Path) : JvmClassPathEntry
}

class JvmClassPathLoader(
    private val entries: List<JvmClassPathEntry>,
    private val methodArea: JvmMethodArea,
) {
    fun load(internalName: String): JvmMethodAreaEntry {
        require(internalName.isNotBlank()) { "class internal name must not be blank" }

        val classBytes = entries.firstNotNullOfOrNull { entry -> entry.findClassBytes(internalName) }
            ?: throw JvmClassPathLookupException("Class $internalName is not present on the classpath")
        val classFile = ClassFileParser.parse(bytes = classBytes.bytes, source = classBytes.source)
        val methodAreaEntry = classFile.toJvmMethodAreaEntry()
        val definedInternalName = methodAreaEntry.definition.internalName
        if (definedInternalName != internalName) {
            throw JvmClassPathNameMismatchException(
                "Class path entry for $internalName defined $definedInternalName instead",
            )
        }
        methodArea.defineClass(methodAreaEntry)
        return methodAreaEntry
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

class JvmClassPathLookupException(message: String) : IllegalStateException(message)

class JvmClassPathNameMismatchException(message: String) : IllegalStateException(message)
