package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

data class OperandStackItem(
    val depth: Int,
    val value: String,
) {
    fun displayText(): String = "$depth: $value"
}

data class OperandStackModel(
    val items: List<OperandStackItem> = emptyList(),
) {
    companion object {
        fun fromFrame(frame: CurrentFrameSnapshot?): OperandStackModel =
            OperandStackModel(
                frame?.operandStack.orEmpty().map { value ->
                    OperandStackItem(
                        depth = value.depth,
                        value = value.value,
                    )
                },
            )
    }
}

object OperandStackViewModel {
    const val Title: String = "Operand Stack"
}

class OperandStackView(
    model: OperandStackModel = OperandStackModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: OperandStackModel) {
        items.setAll(model.items.map(OperandStackItem::displayText))
    }
}
