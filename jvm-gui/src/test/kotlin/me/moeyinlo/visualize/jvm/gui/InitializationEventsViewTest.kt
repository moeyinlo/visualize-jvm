package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InitializationEventsViewTest {
    @Test
    fun `initialization events model formats class initialization state transitions`() {
        val model = InitializationEventsModel.fromEvents(
            listOf(
                InitializationEventSnapshot(
                    sequence = 6,
                    className = "demo/Main",
                    state = InitializationState.Started,
                    trigger = "invokestatic demo/Main.main([Ljava/lang/String;)V",
                ),
                InitializationEventSnapshot(
                    sequence = 7,
                    className = "demo/Main",
                    state = InitializationState.Completed,
                    trigger = "<clinit>()V",
                ),
            ),
        )

        assertEquals(
            listOf(
                InitializationEventItem(
                    sequence = 6,
                    text = "#6 demo/Main initialization started by invokestatic demo/Main.main([Ljava/lang/String;)V",
                ),
                InitializationEventItem(
                    sequence = 7,
                    text = "#7 demo/Main initialization completed by <clinit>()V",
                ),
            ),
            model.items,
        )
    }

    @Test
    fun `initialization events model preserves empty event list`() {
        assertEquals(emptyList(), InitializationEventsModel.fromEvents(emptyList()).items)
    }

    @Test
    fun `initialization events view is exposed as a JavaFX list view type`() {
        assertEquals("Initialization Events", InitializationEventsViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(InitializationEventsView::class.java))
    }
}
