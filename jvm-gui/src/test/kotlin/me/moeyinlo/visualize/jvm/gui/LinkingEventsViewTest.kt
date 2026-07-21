package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkingEventsViewTest {
    @Test
    fun `linking events model formats class linking phase and target`() {
        val model = LinkingEventsModel.fromEvents(
            listOf(
                LinkingEventSnapshot(
                    sequence = 3,
                    className = "demo/Main",
                    phase = LinkingPhase.Verification,
                    target = "demo/Main.main([Ljava/lang/String;)V",
                ),
                LinkingEventSnapshot(
                    sequence = 4,
                    className = "demo/Main",
                    phase = LinkingPhase.Preparation,
                    target = "static fields",
                ),
                LinkingEventSnapshot(
                    sequence = 5,
                    className = "demo/Main",
                    phase = LinkingPhase.Resolution,
                    target = "java/lang/System.out:Ljava/io/PrintStream;",
                ),
            ),
        )

        assertEquals(
            listOf(
                LinkingEventItem(sequence = 3, text = "#3 demo/Main verification linked demo/Main.main([Ljava/lang/String;)V"),
                LinkingEventItem(sequence = 4, text = "#4 demo/Main preparation linked static fields"),
                LinkingEventItem(sequence = 5, text = "#5 demo/Main resolution linked java/lang/System.out:Ljava/io/PrintStream;"),
            ),
            model.items,
        )
    }

    @Test
    fun `linking events model preserves empty event list`() {
        assertEquals(emptyList(), LinkingEventsModel.fromEvents(emptyList()).items)
    }

    @Test
    fun `linking events view is exposed as a JavaFX list view type`() {
        assertEquals("Linking Events", LinkingEventsViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(LinkingEventsView::class.java))
    }
}
