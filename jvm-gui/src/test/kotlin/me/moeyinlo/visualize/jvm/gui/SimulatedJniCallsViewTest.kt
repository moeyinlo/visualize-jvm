package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulatedJniCallsViewTest {
    @Test
    fun `simulated JNI calls model formats function call lifecycle and pending exceptions`() {
        val model = SimulatedJniCallsModel.fromCalls(
            listOf(
                SimulatedJniCallSnapshot(
                    sequence = 29,
                    action = SimulatedJniCallAction.Entered,
                    functionName = "FindClass",
                    localFrameDepth = 1,
                    arguments = listOf("demo/Helper"),
                    result = "pending",
                    pendingException = null,
                ),
                SimulatedJniCallSnapshot(
                    sequence = 30,
                    action = SimulatedJniCallAction.Returned,
                    functionName = "FindClass",
                    localFrameDepth = 1,
                    arguments = listOf("demo/Helper"),
                    result = "jclass#2",
                    pendingException = null,
                ),
                SimulatedJniCallSnapshot(
                    sequence = 31,
                    action = SimulatedJniCallAction.PendingExceptionSet,
                    functionName = "ThrowNew",
                    localFrameDepth = 1,
                    arguments = listOf("java/lang/IllegalStateException", "bad state"),
                    result = "0",
                    pendingException = "java/lang/IllegalStateException",
                ),
                SimulatedJniCallSnapshot(
                    sequence = 32,
                    action = SimulatedJniCallAction.Failed,
                    functionName = "GetMethodID",
                    localFrameDepth = 1,
                    arguments = listOf("jclass#2", "missing", "()V"),
                    result = "NoSuchMethodError",
                    pendingException = "java/lang/NoSuchMethodError",
                ),
            ),
        )

        assertEquals(
            listOf(
                SimulatedJniCallItem(
                    sequence = 29,
                    text = "#29 entered JNI FindClass frame=1 args=[demo/Helper] -> pending pending=none",
                ),
                SimulatedJniCallItem(
                    sequence = 30,
                    text = "#30 returned JNI FindClass frame=1 args=[demo/Helper] -> jclass#2 pending=none",
                ),
                SimulatedJniCallItem(
                    sequence = 31,
                    text = "#31 set pending exception JNI ThrowNew frame=1 args=[java/lang/IllegalStateException, bad state] -> 0 pending=java/lang/IllegalStateException",
                ),
                SimulatedJniCallItem(
                    sequence = 32,
                    text = "#32 failed JNI GetMethodID frame=1 args=[jclass#2, missing, ()V] -> NoSuchMethodError pending=java/lang/NoSuchMethodError",
                ),
            ),
            model.items,
        )
    }

    @Test
    fun `simulated JNI calls model preserves empty call list`() {
        assertEquals(emptyList(), SimulatedJniCallsModel.fromCalls(emptyList()).items)
    }

    @Test
    fun `simulated JNI calls view is exposed as a JavaFX list view type`() {
        assertEquals("Simulated JNI Calls", SimulatedJniCallsViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(SimulatedJniCallsView::class.java))
    }
}
