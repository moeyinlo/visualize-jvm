package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

enum class HostDelegationAction(
    val displayName: String,
) {
    Delegated("delegated"),
    Rejected("rejected"),
    Returned("returned"),
    Failed("failed"),
}

data class HostDelegationEventSnapshot(
    val sequence: Long,
    val action: HostDelegationAction,
    val policy: String,
    val className: String,
    val methodName: String,
    val descriptor: String,
    val detail: String,
)

data class HostDelegationEventItem(
    val sequence: Long,
    val text: String,
)

data class HostDelegationEventsModel(
    val items: List<HostDelegationEventItem> = emptyList(),
) {
    companion object {
        fun fromEvents(events: List<HostDelegationEventSnapshot>): HostDelegationEventsModel =
            HostDelegationEventsModel(
                events.map { event ->
                    HostDelegationEventItem(
                        sequence = event.sequence,
                        text = "#${event.sequence} ${event.action.displayName} " +
                            "${event.className}.${event.methodName}${event.descriptor} " +
                            "via ${event.policy}: ${event.detail}",
                    )
                },
            )
    }
}

object HostDelegationEventsViewModel {
    const val Title: String = "Host Delegation Boundary"
}

class HostDelegationEventsView(
    model: HostDelegationEventsModel = HostDelegationEventsModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: HostDelegationEventsModel) {
        items.setAll(model.items.map(HostDelegationEventItem::text))
    }
}
