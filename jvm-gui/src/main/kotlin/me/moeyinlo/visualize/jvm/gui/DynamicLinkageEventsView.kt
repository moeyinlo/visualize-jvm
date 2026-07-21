package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

enum class DynamicLinkageKind(
    val displayName: String,
) {
    InvokeDynamicLinked("invokedynamic linked"),
    InvokeDynamicInvoked("invokedynamic invoked"),
    ConstantDynamicResolved("condy resolved"),
    ConstantDynamicCached("condy cached"),
}

data class DynamicLinkageEventSnapshot(
    val sequence: Long,
    val kind: DynamicLinkageKind,
    val constantPoolIndex: Int,
    val bootstrapMethod: String,
    val nameAndType: String,
    val descriptor: String,
    val result: String,
    val bytecodeOffset: Int,
)

data class DynamicLinkageEventItem(
    val sequence: Long,
    val text: String,
)

data class DynamicLinkageEventsModel(
    val items: List<DynamicLinkageEventItem> = emptyList(),
) {
    companion object {
        fun fromEvents(events: List<DynamicLinkageEventSnapshot>): DynamicLinkageEventsModel =
            DynamicLinkageEventsModel(
                events.map { event ->
                    DynamicLinkageEventItem(
                        sequence = event.sequence,
                        text = "#${event.sequence} ${event.kind.displayName} cp#${event.constantPoolIndex} " +
                            "${event.nameAndType} ${event.descriptor} via ${event.bootstrapMethod} -> " +
                            "${event.result} @ bci=${event.bytecodeOffset}",
                    )
                },
            )
    }
}

object DynamicLinkageEventsViewModel {
    const val Title: String = "Invokedynamic and Condy Events"
}

class DynamicLinkageEventsView(
    model: DynamicLinkageEventsModel = DynamicLinkageEventsModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: DynamicLinkageEventsModel) {
        items.setAll(model.items.map(DynamicLinkageEventItem::text))
    }
}
