package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

enum class JniUpcallAction(
    val displayName: String,
) {
    EnteredInterpreter("entered interpreter"),
    ReturnedToNative("returned to native"),
    PropagatedGuestException("propagated guest exception"),
}

data class JniUpcallSnapshot(
    val sequence: Long,
    val depth: Int,
    val action: JniUpcallAction,
    val jniFunction: String,
    val targetMethod: String,
    val receiver: String?,
    val arguments: List<String>,
    val result: String,
)

data class JniUpcallItem(
    val sequence: Long,
    val text: String,
)

data class JniUpcallNestingModel(
    val items: List<JniUpcallItem> = emptyList(),
) {
    companion object {
        fun fromUpcalls(upcalls: List<JniUpcallSnapshot>): JniUpcallNestingModel =
            JniUpcallNestingModel(
                upcalls.map { upcall ->
                    JniUpcallItem(
                        sequence = upcall.sequence,
                        text = "#${upcall.sequence} depth=${upcall.depth} ${upcall.action.displayName} " +
                            "via ${upcall.jniFunction} target=${upcall.targetMethod} " +
                            "receiver=${upcall.receiver ?: "static"} args=[${upcall.arguments.joinToString()}] " +
                            "-> ${upcall.result}",
                    )
                },
            )
    }
}

object JniUpcallNestingViewModel {
    const val Title: String = "JNI Upcall Nesting"
}

class JniUpcallNestingView(
    model: JniUpcallNestingModel = JniUpcallNestingModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: JniUpcallNestingModel) {
        items.setAll(model.items.map(JniUpcallItem::text))
    }
}
