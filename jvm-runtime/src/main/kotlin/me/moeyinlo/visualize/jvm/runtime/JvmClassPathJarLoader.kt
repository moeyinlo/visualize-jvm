package me.moeyinlo.visualize.jvm.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import me.moeyinlo.visualize.jvm.classfile.ClassFileParser

class JvmClassPathJarLoader(
    private val jarPath: Path,
    private val methodArea: JvmMethodArea,
) {
    fun load(internalName: String): JvmMethodAreaEntry {
        require(Files.isRegularFile(jarPath)) { "jar classpath entry must be a regular file: $jarPath" }
        require(internalName.isNotBlank()) { "class internal name must not be blank" }

        val entryName = "$internalName.class"
        val classFile = JarFile(jarPath.toFile()).use { jar ->
            val entry = requireNotNull(jar.getJarEntry(entryName)) {
                "class entry $entryName is not present in jar: $jarPath"
            }
            val bytes = jar.getInputStream(entry).use { input -> input.readBytes() }
            ClassFileParser.parse(bytes = bytes, source = "$jarPath!/$entryName")
        }
        val methodAreaEntry = classFile.toJvmMethodAreaEntry()
        methodArea.defineClass(methodAreaEntry)
        return methodAreaEntry
    }
}
