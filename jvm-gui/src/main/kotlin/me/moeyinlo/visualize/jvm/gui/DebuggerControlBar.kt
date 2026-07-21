package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox

enum class DebuggerControlAction {
    Run,
    Step,
    Pause,
    Stop,
}

data class DebuggerControlButton(
    val action: DebuggerControlAction,
    val label: String,
    val enabled: Boolean,
)

object DebuggerControlBarModel {
    const val Title: String = "Debugger"

    fun initialButtons(): List<DebuggerControlButton> =
        listOf(
            DebuggerControlButton(DebuggerControlAction.Run, "Run", enabled = true),
            DebuggerControlButton(DebuggerControlAction.Step, "Step", enabled = false),
            DebuggerControlButton(DebuggerControlAction.Pause, "Pause", enabled = false),
            DebuggerControlButton(DebuggerControlAction.Stop, "Stop", enabled = false),
        )
}

class DebuggerControlBar(
    buttons: List<DebuggerControlButton> = DebuggerControlBarModel.initialButtons(),
) : HBox() {
    init {
        spacing = 8.0
        children += Label(DebuggerControlBarModel.Title)
        children += buttons.map { button ->
            Button(button.label).apply {
                isDisable = !button.enabled
                userData = button.action
            }
        }
    }
}
