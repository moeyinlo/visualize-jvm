package me.moeyinlo.visualize.jvm.gui

import javafx.application.Application
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VisualizeJvmApplicationTest {
    @Test
    fun `application shell exposes JavaFX application metadata`() {
        assertEquals("Visualize JVM", VisualizeJvmApplicationModel.Title)
        assertTrue(Application::class.java.isAssignableFrom(VisualizeJvmApplication::class.java))
    }
}
