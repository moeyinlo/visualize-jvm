package me.moeyinlo.visualize.jvm.gui

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrentInstructionHighlightTest {
    @Test
    fun `bytecode instruction model marks instruction at current frame pc`() {
        val model = BytecodeInstructionModel(
            items = listOf(
                BytecodeInstructionItem(offset = 0, mnemonic = "iconst_1", operands = emptyList()),
                BytecodeInstructionItem(offset = 1, mnemonic = "ireturn", operands = emptyList()),
            ),
        )
        val frame = CurrentFrameSnapshot(
            className = "demo/Main",
            methodName = "value",
            descriptor = "()I",
            pc = 1,
        )

        val highlighted = model.highlightCurrentInstruction(frame)

        assertEquals(
            listOf(
                BytecodeInstructionItem(offset = 0, mnemonic = "iconst_1", operands = emptyList(), isCurrent = false),
                BytecodeInstructionItem(offset = 1, mnemonic = "ireturn", operands = emptyList(), isCurrent = true),
            ),
            highlighted.items,
        )
        assertEquals("=> 0001: ireturn", highlighted.items[1].displayText())
    }

    @Test
    fun `bytecode instruction model clears current marker when no frame is active`() {
        val model = BytecodeInstructionModel(
            items = listOf(
                BytecodeInstructionItem(offset = 0, mnemonic = "return", operands = emptyList(), isCurrent = true),
            ),
        )

        val highlighted = model.highlightCurrentInstruction(null)

        assertEquals(
            listOf(BytecodeInstructionItem(offset = 0, mnemonic = "return", operands = emptyList(), isCurrent = false)),
            highlighted.items,
        )
    }
}
