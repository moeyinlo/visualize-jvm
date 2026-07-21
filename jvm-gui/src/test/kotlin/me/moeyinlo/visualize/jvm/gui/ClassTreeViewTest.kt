package me.moeyinlo.visualize.jvm.gui

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import org.junit.jupiter.api.io.TempDir
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassTreeViewTest {
    @Test
    fun `class tree model discovers classes from jar and class entries`(@TempDir tempDir: Path) {
        val jar = tempDir.resolve("app.jar")
        JarOutputStream(Files.newOutputStream(jar)).use { output ->
            output.putNextEntry(JarEntry("pkg/App.class"))
            output.write(byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte()))
            output.closeEntry()
            output.putNextEntry(JarEntry("pkg/internal/Helper.class"))
            output.write(byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte()))
            output.closeEntry()
            output.putNextEntry(JarEntry("META-INF/MANIFEST.MF"))
            output.write(byteArrayOf())
            output.closeEntry()
        }
        val looseClass = Files.createFile(tempDir.resolve("Loose.class"))

        val model = ClassTreeModel.fromClasspathEntries(listOf(jar, looseClass))

        assertEquals(
            listOf("Loose", "pkg/App", "pkg/internal/Helper"),
            model.classes.map(ClassTreeClassNode::internalName),
        )
    }

    @Test
    fun `class tree view is exposed as a JavaFX tree view type`() {
        assertEquals("Classes", ClassTreeViewModel.Title)
        assertTrue(javafx.scene.control.TreeView::class.java.isAssignableFrom(ClassTreeView::class.java))
    }
}
