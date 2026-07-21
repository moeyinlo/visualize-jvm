package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BytecodeInstructionViewTest {
    @Test
    fun `bytecode instruction model decodes code attribute instructions`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 2,
            maxLocals = 2,
            code = byteArrayOf(
                0x1B.toByte(),
                0x10.toByte(),
                0x2A.toByte(),
                0x60.toByte(),
                0xAC.toByte(),
            ),
        )

        val model = BytecodeInstructionModel.fromCodeAttribute(code)

        assertEquals(
            listOf(
                BytecodeInstructionItem(offset = 0, mnemonic = "iload_1", operands = emptyList()),
                BytecodeInstructionItem(offset = 1, mnemonic = "bipush", operands = listOf(0x2A)),
                BytecodeInstructionItem(offset = 3, mnemonic = "iadd", operands = emptyList()),
                BytecodeInstructionItem(offset = 4, mnemonic = "ireturn", operands = emptyList()),
            ),
            model.items,
        )
    }

    @Test
    fun `bytecode instruction view is exposed as a JavaFX list view type`() {
        assertEquals("Bytecode", BytecodeInstructionViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(BytecodeInstructionView::class.java))
    }
}
