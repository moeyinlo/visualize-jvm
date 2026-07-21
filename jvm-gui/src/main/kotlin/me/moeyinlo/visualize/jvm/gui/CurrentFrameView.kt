package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.Label
import javafx.scene.layout.VBox

data class CurrentFrameSnapshot(
    val className: String,
    val methodName: String,
    val descriptor: String,
    val pc: Int,
    val locals: List<LocalVariableSnapshot> = emptyList(),
    val operandStack: List<OperandStackValueSnapshot> = emptyList(),
)

data class LocalVariableSnapshot(
    val slot: Int,
    val value: String,
)

data class OperandStackValueSnapshot(
    val depth: Int,
    val value: String,
)

data class CurrentFrameModel(
    val frame: CurrentFrameSnapshot?,
    val displayText: String,
) {
    companion object {
        fun fromSnapshot(frame: CurrentFrameSnapshot?): CurrentFrameModel =
            CurrentFrameModel(
                frame = frame,
                displayText = frame?.displayText() ?: "No current frame",
            )

        private fun CurrentFrameSnapshot.displayText(): String =
            "$className.$methodName$descriptor @ pc=$pc"
    }
}

object CurrentFrameViewModel {
    const val Title: String = "Current Frame"
}

class CurrentFrameView(
    model: CurrentFrameModel = CurrentFrameModel.fromSnapshot(null),
) : VBox() {
    private val currentFrameLabel: Label = Label()

    init {
        spacing = 4.0
        children += Label(CurrentFrameViewModel.Title)
        children += currentFrameLabel
        setModel(model)
    }

    fun setModel(model: CurrentFrameModel) {
        currentFrameLabel.text = model.displayText
    }
}
