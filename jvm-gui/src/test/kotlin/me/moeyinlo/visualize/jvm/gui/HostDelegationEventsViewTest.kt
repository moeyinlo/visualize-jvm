package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HostDelegationEventsViewTest {
    @Test
    fun `host delegation events model formats boundary decisions results and failures`() {
        val model = HostDelegationEventsModel.fromEvents(
            listOf(
                HostDelegationEventSnapshot(
                    sequence = 21,
                    action = HostDelegationAction.Delegated,
                    policy = "JDK+whitelist",
                    className = "java/lang/String",
                    methodName = "substring",
                    descriptor = "(II)Ljava/lang/String;",
                    detail = "trusted platform class",
                ),
                HostDelegationEventSnapshot(
                    sequence = 22,
                    action = HostDelegationAction.Rejected,
                    policy = "JDK+whitelist",
                    className = "demo/Main",
                    methodName = "run",
                    descriptor = "()V",
                    detail = "guest class remains interpreted",
                ),
                HostDelegationEventSnapshot(
                    sequence = 23,
                    action = HostDelegationAction.Returned,
                    policy = "JDK+whitelist",
                    className = "java/lang/String",
                    methodName = "substring",
                    descriptor = "(II)Ljava/lang/String;",
                    detail = "guest ref#88",
                ),
                HostDelegationEventSnapshot(
                    sequence = 24,
                    action = HostDelegationAction.Failed,
                    policy = "JDK+whitelist",
                    className = "java/lang/System",
                    methodName = "loadLibrary",
                    descriptor = "(Ljava/lang/String;)V",
                    detail = "blocked native side effect",
                ),
            ),
        )

        assertEquals(
            listOf(
                HostDelegationEventItem(
                    sequence = 21,
                    text = "#21 delegated java/lang/String.substring(II)Ljava/lang/String; via JDK+whitelist: trusted platform class",
                ),
                HostDelegationEventItem(
                    sequence = 22,
                    text = "#22 rejected demo/Main.run()V via JDK+whitelist: guest class remains interpreted",
                ),
                HostDelegationEventItem(
                    sequence = 23,
                    text = "#23 returned java/lang/String.substring(II)Ljava/lang/String; via JDK+whitelist: guest ref#88",
                ),
                HostDelegationEventItem(
                    sequence = 24,
                    text = "#24 failed java/lang/System.loadLibrary(Ljava/lang/String;)V via JDK+whitelist: blocked native side effect",
                ),
            ),
            model.items,
        )
    }

    @Test
    fun `host delegation events model preserves empty event list`() {
        assertEquals(emptyList(), HostDelegationEventsModel.fromEvents(emptyList()).items)
    }

    @Test
    fun `host delegation events view is exposed as a JavaFX list view type`() {
        assertEquals("Host Delegation Boundary", HostDelegationEventsViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(HostDelegationEventsView::class.java))
    }
}
