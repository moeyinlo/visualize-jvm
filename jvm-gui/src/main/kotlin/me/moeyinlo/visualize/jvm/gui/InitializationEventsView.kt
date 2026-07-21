package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

enum class InitializationState(
    val displayName: String,
) {
    Started("started"),
    Completed("completed"),
    Failed("failed"),
}

data class InitializationEventSnapshot(
    val sequence: Long,
    val className: String,
    val state: InitializationState,
    val trigger: String,
)

data class InitializationEventItem(
    val sequence: Long,
    val text: String,
)

data class InitializationEventsModel(
    val items: List<InitializationEventItem> = emptyList(),
) {
    companion object {
        fun fromEvents(events: List<InitializationEventSnapshot>): InitializationEventsModel =
            InitializationEventsModel(
                events.map { event ->
                    InitializationEventItem(
                        sequence = event.sequence,
                        text = "#${event.sequence} ${event.className} initialization " +
                            "${event.state.displayName} by ${event.trigger}",
                    )
                },
            )
    }
}

object InitializationEventsViewModel {
    const val Title: String = "Initialization Events"
}

class InitializationEventsView(
    model: InitializationEventsModel = InitializationEventsModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: InitializationEventsModel) {
        items.setAll(model.items.map(InitializationEventItem::text))
    }
}
