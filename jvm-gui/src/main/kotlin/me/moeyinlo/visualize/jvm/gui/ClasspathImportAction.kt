package me.moeyinlo.visualize.jvm.gui

import java.nio.file.Files
import java.nio.file.Path

data class ClasspathImportResult(
    val project: JvmGuiProjectModel,
    val importedEntries: List<Path>,
    val rejectedEntries: List<Path>,
)

object ClasspathImportAction {
    fun importFiles(
        project: JvmGuiProjectModel,
        selectedFiles: List<Path>,
    ): ClasspathImportResult {
        val existingKeys = project.classpathEntries.mapTo(linkedSetOf()) { entry -> entry.normalizedKey() }
        val updatedEntries = project.classpathEntries.toMutableList()
        val importedEntries = mutableListOf<Path>()
        val rejectedEntries = mutableListOf<Path>()

        selectedFiles.forEach { file ->
            if (!file.isSupportedClasspathFile()) {
                rejectedEntries.add(file)
                return@forEach
            }
            if (existingKeys.add(file.normalizedKey())) {
                updatedEntries.add(file)
                importedEntries.add(file)
            }
        }

        return ClasspathImportResult(
            project = project.copy(classpathEntries = updatedEntries),
            importedEntries = importedEntries,
            rejectedEntries = rejectedEntries,
        )
    }

    private fun Path.isSupportedClasspathFile(): Boolean =
        Files.isRegularFile(this) &&
            fileName?.toString()?.lowercase()?.let { name -> name.endsWith(".jar") || name.endsWith(".class") } == true

    private fun Path.normalizedKey(): Path = toAbsolutePath().normalize()
}
