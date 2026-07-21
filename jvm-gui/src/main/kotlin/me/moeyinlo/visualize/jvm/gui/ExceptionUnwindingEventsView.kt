package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

enum class ExceptionUnwindingAction(
    val displayName: String,
) {
    Thrown("thrown"),
    FrameUnwound("frame unwound"),
    HandlerMatched("handler matched"),
    Uncaught("uncaught"),
}

data class ExceptionUnwindingEventSnapshot(
    val sequence: Long,
    val throwableClassName: String,
    val action: ExceptionUnwindingAction,
    val frame: String,
    val bytecodeOffset: Int,
)

data class ExceptionUnwindingEventItem(
    val sequence: Long,
    val text: String,
)

data class ExceptionUnwindingEventsModel(
    val items: List<ExceptionUnwindingEventItem> = emptyList(),
) {
    companion object {
        fun fromEvents(events: List<ExceptionUnwindingEventSnapshot>): ExceptionUnwindingEventsModel =
            ExceptionUnwindingEventsModel(
                events.map { event ->
                    ExceptionUnwindingEventItem(
                        sequence = event.sequence,
                        text = "#${event.sequence} ${event.throwableClassName} ${event.action.displayName} " +
                            "in ${event.frame} @ bci=${event.bytecodeOffset}",
                    )
                },
            )
    }
}

object ExceptionUnwindingEventsViewModel {
    const val Title: String = "Exception Unwinding"
}

class ExceptionUnwindingEventsView(
    model: ExceptionUnwindingEventsModel = ExceptionUnwindingEventsModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: ExceptionUnwindingEventsModel) {
        items.setAll(model.items.map(ExceptionUnwindingEventItem::text))
    }
}
