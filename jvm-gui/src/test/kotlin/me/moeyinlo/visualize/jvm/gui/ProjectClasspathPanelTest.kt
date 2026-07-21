package me.moeyinlo.visualize.jvm.gui

import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectClasspathPanelTest {
    @Test
    fun `project classpath model preserves ordered entries`() {
        val first = Path.of("libs/a.jar")
        val second = Path.of("classes")

        val model = JvmGuiProjectModel(classpathEntries = listOf(first, second))

        assertEquals(listOf(first, second), model.classpathEntries)
    }

    @Test
    fun `project classpath panel is a reusable JavaFX side panel`() {
        assertEquals("Project / Classpath", ProjectClasspathPanelModel.Title)
        assertTrue(VBox::class.java.isAssignableFrom(ProjectClasspathPanel::class.java))
        assertTrue(BorderPane::class.java.isAssignableFrom(VisualizeJvmRootView::class.java))
    }
}
