package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalVariablesViewTest {
    @Test
    fun `local variables model lists current frame locals by slot index`() {
        val frame = CurrentFrameSnapshot(
            className = "demo/Main",
            methodName = "sum",
            descriptor = "(IJ)J",
            pc = 4,
            locals = listOf(
                LocalVariableSnapshot(slot = 0, value = "this: demo/Main"),
                LocalVariableSnapshot(slot = 1, value = "int 7"),
                LocalVariableSnapshot(slot = 2, value = "long 9"),
                LocalVariableSnapshot(slot = 3, value = "<top>"),
            ),
        )

        val model = LocalVariablesModel.fromFrame(frame)

        assertEquals(
            listOf(
                LocalVariableItem(slot = 0, value = "this: demo/Main"),
                LocalVariableItem(slot = 1, value = "int 7"),
                LocalVariableItem(slot = 2, value = "long 9"),
                LocalVariableItem(slot = 3, value = "<top>"),
            ),
            model.items,
        )
    }

    @Test
    fun `local variables model is empty when no frame is active`() {
        val model = LocalVariablesModel.fromFrame(null)

        assertEquals(emptyList(), model.items)
    }

    @Test
    fun `local variables view is exposed as a JavaFX list view type`() {
        assertEquals("Local Variables", LocalVariablesViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(LocalVariablesView::class.java))
    }
}
