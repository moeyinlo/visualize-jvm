package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConstantPoolViewTest {
    @Test
    fun `constant pool model lists entries and unusable two slot placeholders`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Sample", "Sample".encodeToByteArray()),
                ConstantClassEntry(ConstantPoolIndex(1)),
                ConstantLongEntry(7L),
                ConstantIntegerEntry(42),
            ),
        )

        val model = ConstantPoolModel.fromConstantPool(constantPool)

        assertEquals(
            listOf(
                ConstantPoolItem(index = 1, kind = "Utf8", summary = "Sample"),
                ConstantPoolItem(index = 2, kind = "Class", summary = "name_index=#1"),
                ConstantPoolItem(index = 3, kind = "Long", summary = "7"),
                ConstantPoolItem(index = 4, kind = "Unusable", summary = "two-slot placeholder"),
                ConstantPoolItem(index = 5, kind = "Integer", summary = "42"),
            ),
            model.items,
        )
    }

    @Test
    fun `constant pool view is exposed as a JavaFX list view type`() {
        assertEquals("Constant Pool", ConstantPoolViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(ConstantPoolView::class.java))
    }
}
