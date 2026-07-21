package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassLoadingEventsViewTest {
    @Test
    fun `class loading events model formats loader class and source path`() {
        val model = ClassLoadingEventsModel.fromEvents(
            listOf(
                ClassLoadingEventSnapshot(
                    sequence = 1,
                    loader = "bootstrap",
                    className = "java/lang/Object",
                    source = "host:jrt:/java.base",
                ),
                ClassLoadingEventSnapshot(
                    sequence = 2,
                    loader = "app",
                    className = "demo/Main",
                    source = "file:///tmp/demo/Main.class",
                ),
            ),
        )

        assertEquals(
            listOf(
                ClassLoadingEventItem(sequence = 1, text = "#1 bootstrap loaded java/lang/Object from host:jrt:/java.base"),
                ClassLoadingEventItem(sequence = 2, text = "#2 app loaded demo/Main from file:///tmp/demo/Main.class"),
            ),
            model.items,
        )
    }

    @Test
    fun `class loading events model preserves empty event list`() {
        assertEquals(emptyList(), ClassLoadingEventsModel.fromEvents(emptyList()).items)
    }

    @Test
    fun `class loading events view is exposed as a JavaFX list view type`() {
        assertEquals("Class Loading Events", ClassLoadingEventsViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(ClassLoadingEventsView::class.java))
    }
}
