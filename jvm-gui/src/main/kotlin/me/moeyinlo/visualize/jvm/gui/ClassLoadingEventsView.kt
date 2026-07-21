package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

data class ClassLoadingEventSnapshot(
    val sequence: Long,
    val loader: String,
    val className: String,
    val source: String,
)

data class ClassLoadingEventItem(
    val sequence: Long,
    val text: String,
)

data class ClassLoadingEventsModel(
    val items: List<ClassLoadingEventItem> = emptyList(),
) {
    companion object {
        fun fromEvents(events: List<ClassLoadingEventSnapshot>): ClassLoadingEventsModel =
            ClassLoadingEventsModel(
                events.map { event ->
                    ClassLoadingEventItem(
                        sequence = event.sequence,
                        text = "#${event.sequence} ${event.loader} loaded ${event.className} from ${event.source}",
                    )
                },
            )
    }
}

object ClassLoadingEventsViewModel {
    const val Title: String = "Class Loading Events"
}

class ClassLoadingEventsView(
    model: ClassLoadingEventsModel = ClassLoadingEventsModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: ClassLoadingEventsModel) {
        items.setAll(model.items.map(ClassLoadingEventItem::text))
    }
}
