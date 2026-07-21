package me.moeyinlo.visualize.jvm.gui

import java.nio.file.Path

data class RunConfiguration(
    val mainClassName: String,
    val programArguments: List<String> = emptyList(),
    val classpathEntries: List<Path> = emptyList(),
    val workingDirectory: Path? = null,
) {
    fun isValid(): Boolean = validationErrors().isEmpty()

    fun validationErrors(): List<String> =
        buildList {
            if (mainClassName.isBlank()) {
                add("main class is required")
            }
        }
}

object RunConfigurationModel {
    fun create(
        project: JvmGuiProjectModel,
        mainClassName: String,
        programArguments: List<String> = emptyList(),
        workingDirectory: Path? = null,
    ): RunConfiguration =
        RunConfiguration(
            mainClassName = normalizeMainClassName(mainClassName),
            programArguments = programArguments.toList(),
            classpathEntries = project.classpathEntries.toList(),
            workingDirectory = workingDirectory?.toAbsolutePath()?.normalize(),
        )

    private fun normalizeMainClassName(mainClassName: String): String =
        mainClassName.trim().replace('.', '/')
}
