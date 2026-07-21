package me.moeyinlo.visualize.jvm.gui

import javafx.scene.layout.HBox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebuggerControlBarTest {
    @Test
    fun `debugger control model exposes deterministic action buttons`() {
        assertEquals(
            listOf(
                DebuggerControlButton(DebuggerControlAction.Run, "Run", enabled = true),
                DebuggerControlButton(DebuggerControlAction.Step, "Step", enabled = false),
                DebuggerControlButton(DebuggerControlAction.Pause, "Pause", enabled = false),
                DebuggerControlButton(DebuggerControlAction.Stop, "Stop", enabled = false),
            ),
            DebuggerControlBarModel.initialButtons(),
        )
    }

    @Test
    fun `debugger control bar is exposed as a JavaFX hbox type`() {
        assertEquals("Debugger", DebuggerControlBarModel.Title)
        assertTrue(HBox::class.java.isAssignableFrom(DebuggerControlBar::class.java))
    }
}
