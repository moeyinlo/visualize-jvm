package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExceptionUnwindingEventsViewTest {
    @Test
    fun `exception unwinding events model formats throw unwind handler and uncaught transitions`() {
        val model = ExceptionUnwindingEventsModel.fromEvents(
            listOf(
                ExceptionUnwindingEventSnapshot(
                    sequence = 9,
                    throwableClassName = "java/lang/RuntimeException",
                    action = ExceptionUnwindingAction.Thrown,
                    frame = "demo/Main.fail()V",
                    bytecodeOffset = 4,
                ),
                ExceptionUnwindingEventSnapshot(
                    sequence = 10,
                    throwableClassName = "java/lang/RuntimeException",
                    action = ExceptionUnwindingAction.FrameUnwound,
                    frame = "demo/Main.fail()V",
                    bytecodeOffset = 5,
                ),
                ExceptionUnwindingEventSnapshot(
                    sequence = 11,
                    throwableClassName = "java/lang/RuntimeException",
                    action = ExceptionUnwindingAction.HandlerMatched,
                    frame = "demo/Main.main([Ljava/lang/String;)V",
                    bytecodeOffset = 12,
                ),
                ExceptionUnwindingEventSnapshot(
                    sequence = 12,
                    throwableClassName = "java/lang/Error",
                    action = ExceptionUnwindingAction.Uncaught,
                    frame = "demo/Main.main([Ljava/lang/String;)V",
                    bytecodeOffset = 18,
                ),
            ),
        )

        assertEquals(
            listOf(
                ExceptionUnwindingEventItem(
                    sequence = 9,
                    text = "#9 java/lang/RuntimeException thrown in demo/Main.fail()V @ bci=4",
                ),
                ExceptionUnwindingEventItem(
                    sequence = 10,
                    text = "#10 java/lang/RuntimeException frame unwound in demo/Main.fail()V @ bci=5",
                ),
                ExceptionUnwindingEventItem(
                    sequence = 11,
                    text = "#11 java/lang/RuntimeException handler matched in demo/Main.main([Ljava/lang/String;)V @ bci=12",
                ),
                ExceptionUnwindingEventItem(
                    sequence = 12,
                    text = "#12 java/lang/Error uncaught in demo/Main.main([Ljava/lang/String;)V @ bci=18",
                ),
            ),
            model.items,
        )
    }

    @Test
    fun `exception unwinding events model preserves empty event list`() {
        assertEquals(emptyList(), ExceptionUnwindingEventsModel.fromEvents(emptyList()).items)
    }

    @Test
    fun `exception unwinding events view is exposed as a JavaFX list view type`() {
        assertEquals("Exception Unwinding", ExceptionUnwindingEventsViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(ExceptionUnwindingEventsView::class.java))
    }
}
