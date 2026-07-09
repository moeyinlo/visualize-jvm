package me.moeyinlo.visualize.jvm.oracle

import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class JavapOracleException(message: String) : RuntimeException(message)

data class JavapOutput(
    val command: List<String>,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

object JavapOracle {
    private const val TimeoutSeconds = 30L

    fun verbose(classpathRoot: Path, internalName: String): JavapOutput {
        val normalizedClasspath = classpathRoot.toAbsolutePath().normalize()
        if (!Files.isDirectory(normalizedClasspath)) {
            throw JavapOracleException("javap classpath root does not exist: $normalizedClasspath")
        }

        val binaryName = internalName.toBinaryName()
        return runJavap(
            arguments = listOf("-v", "-classpath", normalizedClasspath.toString(), binaryName),
            displayName = binaryName,
            context = "classpath=$normalizedClasspath",
        )
    }

    fun verbose(classFile: Path): JavapOutput {
        val normalizedClassFile = classFile.toAbsolutePath().normalize()
        if (!Files.isRegularFile(normalizedClassFile)) {
            throw JavapOracleException("javap input class file does not exist: $normalizedClassFile")
        }

        return runJavap(
            arguments = listOf("-v", normalizedClassFile.toString()),
            displayName = normalizedClassFile.toString(),
            context = "classFile=$normalizedClassFile",
        )
    }

    private fun runJavap(
        arguments: List<String>,
        displayName: String,
        context: String,
    ): JavapOutput {
        val command = listOf(javapCommand()) + arguments
        val process = try {
            ProcessBuilder(command).start()
        } catch (exception: IOException) {
            throw JavapOracleException("failed to start javap -v for $displayName ($context): ${exception.message}")
        }

        process.outputStream.close()

        val charset = Charset.defaultCharset()
        val stdout = CompletableFuture.supplyAsync<String> { String(process.inputStream.readBytes(), charset) }
        val stderr = CompletableFuture.supplyAsync<String> { String(process.errorStream.readBytes(), charset) }

        if (!process.waitFor(TimeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            throw JavapOracleException("javap -v timed out for $displayName ($context)")
        }

        val output = JavapOutput(
            command = command,
            exitCode = process.exitValue(),
            stdout = stdout.get(5, TimeUnit.SECONDS),
            stderr = stderr.get(5, TimeUnit.SECONDS),
        )

        if (output.exitCode != 0) {
            throw JavapOracleException(output.formatFailure(displayName, context))
        }

        return output
    }

    private fun javapCommand(): String {
        val executableName = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            "javap.exe"
        } else {
            "javap"
        }

        val javaHome = Path.of(System.getProperty("java.home")).toAbsolutePath().normalize()
        val candidates = buildList {
            add(javaHome.resolve("bin").resolve(executableName))
            javaHome.parent?.let { add(it.resolve("bin").resolve(executableName)) }
        }

        return candidates.firstOrNull(Files::isRegularFile)?.toString() ?: "javap"
    }

    private fun String.toBinaryName(): String {
        val trimmed = trim()
        if (trimmed.isBlank()) {
            throw JavapOracleException("javap class name is blank")
        }
        if (trimmed.startsWith("/") || trimmed.startsWith("\\") || trimmed.contains("..")) {
            throw JavapOracleException("javap class name is not a valid fixture class name: $this")
        }
        return trimmed
            .removeSuffix(".class")
            .replace('/', '.')
            .replace('\\', '.')
    }

    private fun JavapOutput.formatFailure(displayName: String, context: String): String = buildString {
        appendLine("javap -v failed for $displayName")
        appendLine("Context: $context")
        appendLine("Exit code: $exitCode")
        appendLine("Command: ${command.joinToString(" ")}")
        appendLine("stdout:")
        appendLine(stdout.ifBlank { "<empty>" })
        appendLine("stderr:")
        appendLine(stderr.ifBlank { "<empty>" })
    }
}
