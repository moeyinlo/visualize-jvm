package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.interpreter.BytecodeDecoder

data class BytecodeInstructionItem(
    val offset: Int,
    val mnemonic: String,
    val operands: List<Int>,
    val isCurrent: Boolean = false,
) {
    fun displayText(): String {
        val operandText = operands.joinToString(separator = " ") { operand ->
            "0x${operand.toString(16).padStart(2, '0')}"
        }
        val instructionText = if (operandText.isEmpty()) {
            "%04d: %s".format(offset, mnemonic)
        } else {
            "%04d: %s %s".format(offset, mnemonic, operandText)
        }
        return if (isCurrent) "=> $instructionText" else instructionText
    }
}

data class BytecodeInstructionModel(
    val items: List<BytecodeInstructionItem> = emptyList(),
) {
    fun highlightCurrentInstruction(frame: CurrentFrameSnapshot?): BytecodeInstructionModel =
        BytecodeInstructionModel(
            items = items.map { item ->
                item.copy(isCurrent = frame?.pc == item.offset)
            },
        )

    companion object {
        fun fromCodeAttribute(codeAttribute: CodeAttribute): BytecodeInstructionModel =
            BytecodeInstructionModel(
                BytecodeDecoder.decode(codeAttribute.code).map { instruction ->
                    BytecodeInstructionItem(
                        offset = instruction.offset,
                        mnemonic = instruction.metadata.mnemonic,
                        operands = instruction.operands,
                    )
                },
            )
    }
}

object BytecodeInstructionViewModel {
    const val Title: String = "Bytecode"
}

class BytecodeInstructionView(
    model: BytecodeInstructionModel = BytecodeInstructionModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: BytecodeInstructionModel) {
        items.setAll(model.items.map(BytecodeInstructionItem::displayText))
    }
}
