package me.moeyinlo.visualize.jvm.oracle

import java.nio.file.Files
import java.nio.file.Path
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider

class JavaFixtureCompilationException(message: String) : RuntimeException(message)

data class CompiledJavaFixture(
    val outputDirectory: Path,
    val classFiles: List<Path>,
) {
    fun readClassBytes(internalName: String): ByteArray {
        val classFile = outputDirectory.resolve("$internalName.class").normalize()
        require(classFile.startsWith(outputDirectory.normalize())) {
            "Class name escapes fixture output directory: $internalName"
        }
        require(Files.isRegularFile(classFile)) {
            "Class file not found for internal name: $internalName"
        }
        return Files.readAllBytes(classFile)
    }
}

object JavaFixtureCompiler {
    fun compile(
        sourceName: String,
        source: String,
        outputDirectory: Path = Files.createTempDirectory("visualize-jvm-fixture"),
        options: List<String> = emptyList(),
    ): CompiledJavaFixture {
        val normalizedOutput = outputDirectory.toAbsolutePath().normalize()
        Files.createDirectories(normalizedOutput)

        val sourcePath = normalizedOutput.resolve(sourceName).normalize()
        require(sourcePath.startsWith(normalizedOutput)) {
            "Source name escapes fixture output directory: $sourceName"
        }
        sourcePath.parent?.let(Files::createDirectories)
        Files.writeString(sourcePath, source)

        val compiler = ToolProvider.getSystemJavaCompiler()
            ?: throw JavaFixtureCompilationException("A JDK compiler is required to compile Java fixtures")
        val diagnostics = DiagnosticCollector<JavaFileObject>()

        compiler.getStandardFileManager(diagnostics, null, null).use { fileManager ->
            val compilationUnits = fileManager.getJavaFileObjectsFromFiles(listOf(sourcePath.toFile()))
            val compilerOptions = buildList {
                add("-d")
                add(normalizedOutput.toString())
                addAll(options)
            }
            val success = compiler.getTask(null, fileManager, diagnostics, compilerOptions, null, compilationUnits).call()
            if (!success) {
                throw JavaFixtureCompilationException(formatDiagnostics(diagnostics))
            }
        }

        val classFiles = Files.walk(normalizedOutput).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".class") }
                .sorted()
                .toList()
        }
        return CompiledJavaFixture(normalizedOutput, classFiles)
    }

    private fun formatDiagnostics(diagnostics: DiagnosticCollector<JavaFileObject>): String {
        val messages = diagnostics.diagnostics.joinToString(separator = System.lineSeparator()) { diagnostic ->
            val source = diagnostic.source?.name ?: "<unknown source>"
            "$source:${diagnostic.lineNumber}:${diagnostic.columnNumber}: ${diagnostic.kind}: ${diagnostic.getMessage(null)}"
        }
        return if (messages.isBlank()) "Java fixture compilation failed" else messages
    }
}
