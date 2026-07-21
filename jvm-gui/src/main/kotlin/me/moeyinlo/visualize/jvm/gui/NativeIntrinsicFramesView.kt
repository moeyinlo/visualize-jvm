package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

enum class NativeIntrinsicFrameAction(
    val displayName: String,
) {
    Entered("entered"),
    Returned("returned"),
    Threw("threw"),
    FellBackToSimulatedJni("fell back to simulated JNI"),
}

data class NativeIntrinsicFrameSnapshot(
    val sequence: Long,
    val depth: Int,
    val action: NativeIntrinsicFrameAction,
    val intrinsicName: String,
    val guestMethod: String,
    val detail: String,
)

data class NativeIntrinsicFrameItem(
    val sequence: Long,
    val text: String,
)

data class NativeIntrinsicFramesModel(
    val items: List<NativeIntrinsicFrameItem> = emptyList(),
) {
    companion object {
        fun fromFrames(frames: List<NativeIntrinsicFrameSnapshot>): NativeIntrinsicFramesModel =
            NativeIntrinsicFramesModel(
                frames.map { frame ->
                    NativeIntrinsicFrameItem(
                        sequence = frame.sequence,
                        text = "#${frame.sequence} depth=${frame.depth} ${frame.action.displayName} " +
                            "intrinsic ${frame.intrinsicName} for ${frame.guestMethod}: ${frame.detail}",
                    )
                },
            )
    }
}

object NativeIntrinsicFramesViewModel {
    const val Title: String = "Native Intrinsic Frames"
}

class NativeIntrinsicFramesView(
    model: NativeIntrinsicFramesModel = NativeIntrinsicFramesModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: NativeIntrinsicFramesModel) {
        items.setAll(model.items.map(NativeIntrinsicFrameItem::text))
    }
}
