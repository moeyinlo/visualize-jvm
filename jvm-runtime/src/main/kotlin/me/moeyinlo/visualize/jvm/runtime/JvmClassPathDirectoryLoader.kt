package me.moeyinlo.visualize.jvm.runtime

import java.nio.file.Files
import java.nio.file.Path

class JvmClassPathDirectoryLoader(
    private val classpathRoot: Path,
    methodArea: JvmMethodArea,
) {
    private val classFilePathLoader = JvmClassFilePathLoader(methodArea)

    fun load(internalName: String): JvmMethodAreaEntry {
        require(Files.isDirectory(classpathRoot)) { "classpath root must be a directory: $classpathRoot" }
        require(internalName.isNotBlank()) { "class internal name must not be blank" }

        val classFilePath = classpathRoot.resolve("$internalName.class")
        return classFilePathLoader.load(classFilePath)
    }
}
