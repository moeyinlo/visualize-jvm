package me.moeyinlo.visualize.jvm.gui

import javafx.scene.layout.VBox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurrentFrameViewTest {
    @Test
    fun `current frame model formats the selected runtime frame`() {
        val frame = CurrentFrameSnapshot(
            className = "demo/Main",
            methodName = "sum",
            descriptor = "(II)I",
            pc = 7,
        )

        val model = CurrentFrameModel.fromSnapshot(frame)

        assertEquals("demo/Main.sum(II)I @ pc=7", model.displayText)
        assertEquals(frame, model.frame)
    }

    @Test
    fun `current frame model reports empty state when no frame is active`() {
        val model = CurrentFrameModel.fromSnapshot(null)

        assertEquals("No current frame", model.displayText)
        assertEquals(null, model.frame)
    }

    @Test
    fun `current frame view is exposed as a JavaFX vbox type`() {
        assertEquals("Current Frame", CurrentFrameViewModel.Title)
        assertTrue(VBox::class.java.isAssignableFrom(CurrentFrameView::class.java))
    }
}
