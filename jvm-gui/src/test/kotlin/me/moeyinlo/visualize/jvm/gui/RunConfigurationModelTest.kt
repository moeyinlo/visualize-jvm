package me.moeyinlo.visualize.jvm.gui

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunConfigurationModelTest {
    @Test
    fun `run configuration derives classpath and normalizes main class`() {
        val project = JvmGuiProjectModel(
            classpathEntries = listOf(Path.of("app.jar"), Path.of("classes")),
        )

        val configuration = RunConfigurationModel.create(
            project = project,
            mainClassName = " demo.Main ",
            programArguments = listOf("one", "two"),
            workingDirectory = Path.of("."),
        )

        assertEquals("demo/Main", configuration.mainClassName)
        assertEquals(listOf("one", "two"), configuration.programArguments)
        assertEquals(project.classpathEntries, configuration.classpathEntries)
        assertEquals(Path.of(".").toAbsolutePath().normalize(), configuration.workingDirectory)
    }

    @Test
    fun `run configuration validation rejects missing main class`() {
        val invalid = RunConfigurationModel.create(
            project = JvmGuiProjectModel(),
            mainClassName = " ",
        )

        assertFalse(invalid.isValid())
        assertEquals(listOf("main class is required"), invalid.validationErrors())
    }

    @Test
    fun `run configuration validation accepts normalized main class`() {
        val valid = RunConfigurationModel.create(
            project = JvmGuiProjectModel(),
            mainClassName = "demo/Main",
        )

        assertTrue(valid.isValid())
        assertEquals(emptyList(), valid.validationErrors())
    }
}
