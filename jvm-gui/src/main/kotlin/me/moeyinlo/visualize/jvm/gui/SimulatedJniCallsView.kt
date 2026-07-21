package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

enum class SimulatedJniCallAction(
    val displayName: String,
) {
    Entered("entered"),
    Returned("returned"),
    PendingExceptionSet("set pending exception"),
    Failed("failed"),
}

data class SimulatedJniCallSnapshot(
    val sequence: Long,
    val action: SimulatedJniCallAction,
    val functionName: String,
    val localFrameDepth: Int,
    val arguments: List<String>,
    val result: String,
    val pendingException: String?,
)

data class SimulatedJniCallItem(
    val sequence: Long,
    val text: String,
)

data class SimulatedJniCallsModel(
    val items: List<SimulatedJniCallItem> = emptyList(),
) {
    companion object {
        fun fromCalls(calls: List<SimulatedJniCallSnapshot>): SimulatedJniCallsModel =
            SimulatedJniCallsModel(
                calls.map { call ->
                    SimulatedJniCallItem(
                        sequence = call.sequence,
                        text = "#${call.sequence} ${call.action.displayName} JNI ${call.functionName} " +
                            "frame=${call.localFrameDepth} args=[${call.arguments.joinToString()}] -> " +
                            "${call.result} pending=${call.pendingException ?: "none"}",
                    )
                },
            )
    }
}

object SimulatedJniCallsViewModel {
    const val Title: String = "Simulated JNI Calls"
}

class SimulatedJniCallsView(
    model: SimulatedJniCallsModel = SimulatedJniCallsModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: SimulatedJniCallsModel) {
        items.setAll(model.items.map(SimulatedJniCallItem::text))
    }
}
