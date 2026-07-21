package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicLinkageEventsViewTest {
    @Test
    fun `dynamic linkage events model formats invokedynamic and condy transitions`() {
        val model = DynamicLinkageEventsModel.fromEvents(
            listOf(
                DynamicLinkageEventSnapshot(
                    sequence = 17,
                    kind = DynamicLinkageKind.InvokeDynamicLinked,
                    constantPoolIndex = 21,
                    bootstrapMethod = "java/lang/invoke/LambdaMetafactory.metafactory",
                    nameAndType = "run:()Ljava/lang/Runnable;",
                    descriptor = "()Ljava/lang/Runnable;",
                    result = "CallSite[target=demo/Main.lambda_main_0()V]",
                    bytecodeOffset = 6,
                ),
                DynamicLinkageEventSnapshot(
                    sequence = 18,
                    kind = DynamicLinkageKind.InvokeDynamicInvoked,
                    constantPoolIndex = 21,
                    bootstrapMethod = "java/lang/invoke/LambdaMetafactory.metafactory",
                    nameAndType = "run:()Ljava/lang/Runnable;",
                    descriptor = "()Ljava/lang/Runnable;",
                    result = "ref#77",
                    bytecodeOffset = 6,
                ),
                DynamicLinkageEventSnapshot(
                    sequence = 19,
                    kind = DynamicLinkageKind.ConstantDynamicResolved,
                    constantPoolIndex = 31,
                    bootstrapMethod = "demo/Bootstrap.bootstrapConstant",
                    nameAndType = "answer:I",
                    descriptor = "I",
                    result = "int:42",
                    bytecodeOffset = 14,
                ),
                DynamicLinkageEventSnapshot(
                    sequence = 20,
                    kind = DynamicLinkageKind.ConstantDynamicCached,
                    constantPoolIndex = 31,
                    bootstrapMethod = "demo/Bootstrap.bootstrapConstant",
                    nameAndType = "answer:I",
                    descriptor = "I",
                    result = "int:42",
                    bytecodeOffset = 20,
                ),
            ),
        )

        assertEquals(
            listOf(
                DynamicLinkageEventItem(
                    sequence = 17,
                    text = "#17 invokedynamic linked cp#21 run:()Ljava/lang/Runnable; ()Ljava/lang/Runnable; via java/lang/invoke/LambdaMetafactory.metafactory -> CallSite[target=demo/Main.lambda_main_0()V] @ bci=6",
                ),
                DynamicLinkageEventItem(
                    sequence = 18,
                    text = "#18 invokedynamic invoked cp#21 run:()Ljava/lang/Runnable; ()Ljava/lang/Runnable; via java/lang/invoke/LambdaMetafactory.metafactory -> ref#77 @ bci=6",
                ),
                DynamicLinkageEventItem(
                    sequence = 19,
                    text = "#19 condy resolved cp#31 answer:I I via demo/Bootstrap.bootstrapConstant -> int:42 @ bci=14",
                ),
                DynamicLinkageEventItem(
                    sequence = 20,
                    text = "#20 condy cached cp#31 answer:I I via demo/Bootstrap.bootstrapConstant -> int:42 @ bci=20",
                ),
            ),
            model.items,
        )
    }

    @Test
    fun `dynamic linkage events model preserves empty event list`() {
        assertEquals(emptyList(), DynamicLinkageEventsModel.fromEvents(emptyList()).items)
    }

    @Test
    fun `dynamic linkage events view is exposed as a JavaFX list view type`() {
        assertEquals("Invokedynamic and Condy Events", DynamicLinkageEventsViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(DynamicLinkageEventsView::class.java))
    }
}
