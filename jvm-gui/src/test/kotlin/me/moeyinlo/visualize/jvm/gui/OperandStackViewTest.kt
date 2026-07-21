package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperandStackViewTest {
    @Test
    fun `operand stack model lists current frame stack values from bottom to top`() {
        val frame = CurrentFrameSnapshot(
            className = "demo/Main",
            methodName = "compute",
            descriptor = "()I",
            pc = 3,
            operandStack = listOf(
                OperandStackValueSnapshot(depth = 0, value = "int 1"),
                OperandStackValueSnapshot(depth = 1, value = "int 2"),
            ),
        )

        val model = OperandStackModel.fromFrame(frame)

        assertEquals(
            listOf(
                OperandStackItem(depth = 0, value = "int 1"),
                OperandStackItem(depth = 1, value = "int 2"),
            ),
            model.items,
        )
    }

    @Test
    fun `operand stack model is empty when no frame is active`() {
        val model = OperandStackModel.fromFrame(null)

        assertEquals(emptyList(), model.items)
    }

    @Test
    fun `operand stack view is exposed as a JavaFX list view type`() {
        assertEquals("Operand Stack", OperandStackViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(OperandStackView::class.java))
    }
}
