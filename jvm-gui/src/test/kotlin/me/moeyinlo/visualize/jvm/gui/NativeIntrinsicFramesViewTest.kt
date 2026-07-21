package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeIntrinsicFramesViewTest {
    @Test
    fun `native intrinsic frame model formats enter return throw and fallback transitions`() {
        val model = NativeIntrinsicFramesModel.fromFrames(
            listOf(
                NativeIntrinsicFrameSnapshot(
                    sequence = 25,
                    depth = 1,
                    action = NativeIntrinsicFrameAction.Entered,
                    intrinsicName = "java/lang/Object.hashCode()I",
                    guestMethod = "java/lang/Object.hashCode()I",
                    detail = "receiver=ref#1",
                ),
                NativeIntrinsicFrameSnapshot(
                    sequence = 26,
                    depth = 1,
                    action = NativeIntrinsicFrameAction.Returned,
                    intrinsicName = "java/lang/System.identityHashCode(Ljava/lang/Object;)I",
                    guestMethod = "java/lang/System.identityHashCode(Ljava/lang/Object;)I",
                    detail = "int:1234",
                ),
                NativeIntrinsicFrameSnapshot(
                    sequence = 27,
                    depth = 1,
                    action = NativeIntrinsicFrameAction.Threw,
                    intrinsicName = "java/lang/Object.clone()Ljava/lang/Object;",
                    guestMethod = "java/lang/Object.clone()Ljava/lang/Object;",
                    detail = "java/lang/CloneNotSupportedException",
                ),
                NativeIntrinsicFrameSnapshot(
                    sequence = 28,
                    depth = 0,
                    action = NativeIntrinsicFrameAction.FellBackToSimulatedJni,
                    intrinsicName = "missing",
                    guestMethod = "demo/Native.a()V",
                    detail = "no Kotlin intrinsic registered",
                ),
            ),
        )

        assertEquals(
            listOf(
                NativeIntrinsicFrameItem(
                    sequence = 25,
                    text = "#25 depth=1 entered intrinsic java/lang/Object.hashCode()I for java/lang/Object.hashCode()I: receiver=ref#1",
                ),
                NativeIntrinsicFrameItem(
                    sequence = 26,
                    text = "#26 depth=1 returned intrinsic java/lang/System.identityHashCode(Ljava/lang/Object;)I for java/lang/System.identityHashCode(Ljava/lang/Object;)I: int:1234",
                ),
                NativeIntrinsicFrameItem(
                    sequence = 27,
                    text = "#27 depth=1 threw intrinsic java/lang/Object.clone()Ljava/lang/Object; for java/lang/Object.clone()Ljava/lang/Object;: java/lang/CloneNotSupportedException",
                ),
                NativeIntrinsicFrameItem(
                    sequence = 28,
                    text = "#28 depth=0 fell back to simulated JNI intrinsic missing for demo/Native.a()V: no Kotlin intrinsic registered",
                ),
            ),
            model.items,
        )
    }

    @Test
    fun `native intrinsic frame model preserves empty frame list`() {
        assertEquals(emptyList(), NativeIntrinsicFramesModel.fromFrames(emptyList()).items)
    }

    @Test
    fun `native intrinsic frames view is exposed as a JavaFX list view type`() {
        assertEquals("Native Intrinsic Frames", NativeIntrinsicFramesViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(NativeIntrinsicFramesView::class.java))
    }
}
