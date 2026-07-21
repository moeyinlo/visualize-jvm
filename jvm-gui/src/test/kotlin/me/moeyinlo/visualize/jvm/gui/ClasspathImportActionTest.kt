package me.moeyinlo.visualize.jvm.gui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.io.TempDir
import kotlin.test.Test
import kotlin.test.assertEquals

class ClasspathImportActionTest {
    @Test
    fun `classpath import action accepts jar and class files in order`(@TempDir tempDir: Path) {
        val jar = Files.createFile(tempDir.resolve("sample.jar"))
        val clazz = Files.createFile(tempDir.resolve("Sample.class"))
        val text = Files.createFile(tempDir.resolve("notes.txt"))
        val project = JvmGuiProjectModel(classpathEntries = listOf(jar))

        val result = ClasspathImportAction.importFiles(project, listOf(jar, clazz, text))

        assertEquals(JvmGuiProjectModel(classpathEntries = listOf(jar, clazz)), result.project)
        assertEquals(listOf(clazz), result.importedEntries)
        assertEquals(listOf(text), result.rejectedEntries)
    }

    @Test
    fun `project classpath panel exposes jar and class import action API`() {
        assertEquals("Import JAR/Class", ProjectClasspathPanelModel.ImportButtonText)
        assertEquals(
            "importClasspathEntries",
            ProjectClasspathPanel::class.java.getMethod("importClasspathEntries", List::class.java).name,
        )
    }
}
