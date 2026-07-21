package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

enum class MonitorAction(
    val displayName: String,
) {
    Entered("entered"),
    Reentered("re-entered"),
    Exited("exited"),
    ExitFailed("failed to exit"),
}

data class MonitorEventSnapshot(
    val sequence: Long,
    val action: MonitorAction,
    val objectReference: String,
    val threadId: String,
    val holdCount: Int,
    val frame: String,
    val bytecodeOffset: Int,
)

data class MonitorEventItem(
    val sequence: Long,
    val text: String,
)

data class MonitorEventsModel(
    val items: List<MonitorEventItem> = emptyList(),
) {
    companion object {
        fun fromEvents(events: List<MonitorEventSnapshot>): MonitorEventsModel =
            MonitorEventsModel(
                events.map { event ->
                    MonitorEventItem(
                        sequence = event.sequence,
                        text = "#${event.sequence} ${event.action.displayName} monitor ${event.objectReference} " +
                            "on thread ${event.threadId} hold=${event.holdCount} in ${event.frame} " +
                            "@ bci=${event.bytecodeOffset}",
                    )
                },
            )
    }
}

object MonitorEventsViewModel {
    const val Title: String = "Monitor Events"
}

class MonitorEventsView(
    model: MonitorEventsModel = MonitorEventsModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: MonitorEventsModel) {
        items.setAll(model.items.map(MonitorEventItem::text))
    }
}
