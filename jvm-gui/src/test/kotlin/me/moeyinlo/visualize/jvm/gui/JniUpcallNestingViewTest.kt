package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JniUpcallNestingViewTest {
    @Test
    fun `JNI upcall nesting model formats interpreter reentry lifecycle`() {
        val model = JniUpcallNestingModel.fromUpcalls(
            listOf(
                JniUpcallSnapshot(
                    sequence = 33,
                    depth = 1,
                    action = JniUpcallAction.EnteredInterpreter,
                    jniFunction = "CallStaticObjectMethod",
                    targetMethod = "demo/Helper.make()Ljava/lang/Object;",
                    receiver = null,
                    arguments = listOf("int:7"),
                    result = "pending",
                ),
                JniUpcallSnapshot(
                    sequence = 34,
                    depth = 2,
                    action = JniUpcallAction.EnteredInterpreter,
                    jniFunction = "CallVoidMethod",
                    targetMethod = "demo/Callback.run()V",
                    receiver = "ref#9",
                    arguments = emptyList(),
                    result = "pending",
                ),
                JniUpcallSnapshot(
                    sequence = 35,
                    depth = 2,
                    action = JniUpcallAction.ReturnedToNative,
                    jniFunction = "CallVoidMethod",
                    targetMethod = "demo/Callback.run()V",
                    receiver = "ref#9",
                    arguments = emptyList(),
                    result = "void",
                ),
                JniUpcallSnapshot(
                    sequence = 36,
                    depth = 1,
                    action = JniUpcallAction.PropagatedGuestException,
                    jniFunction = "CallStaticObjectMethod",
                    targetMethod = "demo/Helper.make()Ljava/lang/Object;",
                    receiver = null,
                    arguments = listOf("int:7"),
                    result = "java/lang/IllegalStateException",
                ),
            ),
        )

        assertEquals(
            listOf(
                JniUpcallItem(
                    sequence = 33,
                    text = "#33 depth=1 entered interpreter via CallStaticObjectMethod target=demo/Helper.make()Ljava/lang/Object; receiver=static args=[int:7] -> pending",
                ),
                JniUpcallItem(
                    sequence = 34,
                    text = "#34 depth=2 entered interpreter via CallVoidMethod target=demo/Callback.run()V receiver=ref#9 args=[] -> pending",
                ),
                JniUpcallItem(
                    sequence = 35,
                    text = "#35 depth=2 returned to native via CallVoidMethod target=demo/Callback.run()V receiver=ref#9 args=[] -> void",
                ),
                JniUpcallItem(
                    sequence = 36,
                    text = "#36 depth=1 propagated guest exception via CallStaticObjectMethod target=demo/Helper.make()Ljava/lang/Object; receiver=static args=[int:7] -> java/lang/IllegalStateException",
                ),
            ),
            model.items,
        )
    }

    @Test
    fun `JNI upcall nesting model preserves empty upcall list`() {
        assertEquals(emptyList(), JniUpcallNestingModel.fromUpcalls(emptyList()).items)
    }

    @Test
    fun `JNI upcall nesting view is exposed as a JavaFX list view type`() {
        assertEquals("JNI Upcall Nesting", JniUpcallNestingViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(JniUpcallNestingView::class.java))
    }
}
