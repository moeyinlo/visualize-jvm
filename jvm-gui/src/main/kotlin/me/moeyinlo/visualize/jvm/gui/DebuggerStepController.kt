package me.moeyinlo.visualize.jvm.gui

fun interface DebuggerStepEngine {
    fun step(configuration: RunConfiguration): DebuggerStepResult
}

data class DebuggerStepResult(
    val stepIndex: Int,
)

class DebuggerStepController(
    private val runConfiguration: RunConfiguration,
    private val engine: DebuggerStepEngine,
) {
    fun handle(action: DebuggerControlAction): DebuggerStepResult? =
        when (action) {
            DebuggerControlAction.Step -> engine.step(runConfiguration)
            DebuggerControlAction.Run,
            DebuggerControlAction.Pause,
            DebuggerControlAction.Stop,
            -> null
        }
}
