package me.moeyinlo.visualize.jvm.runtime

import java.nio.file.Files
import java.nio.file.Path
import me.moeyinlo.visualize.jvm.classfile.ClassFileParser

class JvmClassFilePathLoader(
    private val methodArea: JvmMethodArea,
) {
    fun load(classFilePath: Path): JvmMethodAreaEntry {
        require(Files.isRegularFile(classFilePath)) { "class file path must be a regular file: $classFilePath" }
        val bytes = Files.readAllBytes(classFilePath)
        val classFile = ClassFileParser.parse(bytes = bytes, source = classFilePath.toString())
        val entry = classFile.toJvmMethodAreaEntry()
        methodArea.defineClass(entry)
        return entry
    }
}
