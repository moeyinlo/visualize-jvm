package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonitorEventsViewTest {
    @Test
    fun `monitor events model formats enter reenter exit and failure transitions`() {
        val model = MonitorEventsModel.fromEvents(
            listOf(
                MonitorEventSnapshot(
                    sequence = 13,
                    action = MonitorAction.Entered,
                    objectReference = "ref#42",
                    threadId = "main",
                    holdCount = 1,
                    frame = "demo/Main.lock()V",
                    bytecodeOffset = 3,
                ),
                MonitorEventSnapshot(
                    sequence = 14,
                    action = MonitorAction.Reentered,
                    objectReference = "ref#42",
                    threadId = "main",
                    holdCount = 2,
                    frame = "demo/Main.lockAgain()V",
                    bytecodeOffset = 7,
                ),
                MonitorEventSnapshot(
                    sequence = 15,
                    action = MonitorAction.Exited,
                    objectReference = "ref#42",
                    threadId = "main",
                    holdCount = 1,
                    frame = "demo/Main.unlock()V",
                    bytecodeOffset = 11,
                ),
                MonitorEventSnapshot(
                    sequence = 16,
                    action = MonitorAction.ExitFailed,
                    objectReference = "ref#43",
                    threadId = "worker",
                    holdCount = 0,
                    frame = "demo/Main.badUnlock()V",
                    bytecodeOffset = 19,
                ),
            ),
        )

        assertEquals(
            listOf(
                MonitorEventItem(
                    sequence = 13,
                    text = "#13 entered monitor ref#42 on thread main hold=1 in demo/Main.lock()V @ bci=3",
                ),
                MonitorEventItem(
                    sequence = 14,
                    text = "#14 re-entered monitor ref#42 on thread main hold=2 in demo/Main.lockAgain()V @ bci=7",
                ),
                MonitorEventItem(
                    sequence = 15,
                    text = "#15 exited monitor ref#42 on thread main hold=1 in demo/Main.unlock()V @ bci=11",
                ),
                MonitorEventItem(
                    sequence = 16,
                    text = "#16 failed to exit monitor ref#43 on thread worker hold=0 in demo/Main.badUnlock()V @ bci=19",
                ),
            ),
            model.items,
        )
    }

    @Test
    fun `monitor events model preserves empty event list`() {
        assertEquals(emptyList(), MonitorEventsModel.fromEvents(emptyList()).items)
    }

    @Test
    fun `monitor events view is exposed as a JavaFX list view type`() {
        assertEquals("Monitor Events", MonitorEventsViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(MonitorEventsView::class.java))
    }
}
