package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

enum class LinkingPhase(
    val displayName: String,
) {
    Verification("verification"),
    Preparation("preparation"),
    Resolution("resolution"),
}

data class LinkingEventSnapshot(
    val sequence: Long,
    val className: String,
    val phase: LinkingPhase,
    val target: String,
)

data class LinkingEventItem(
    val sequence: Long,
    val text: String,
)

data class LinkingEventsModel(
    val items: List<LinkingEventItem> = emptyList(),
) {
    companion object {
        fun fromEvents(events: List<LinkingEventSnapshot>): LinkingEventsModel =
            LinkingEventsModel(
                events.map { event ->
                    LinkingEventItem(
                        sequence = event.sequence,
                        text = "#${event.sequence} ${event.className} ${event.phase.displayName} linked ${event.target}",
                    )
                },
            )
    }
}

object LinkingEventsViewModel {
    const val Title: String = "Linking Events"
}

class LinkingEventsView(
    model: LinkingEventsModel = LinkingEventsModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: LinkingEventsModel) {
        items.setAll(model.items.map(LinkingEventItem::text))
    }
}
