package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

data class LocalVariableItem(
    val slot: Int,
    val value: String,
) {
    fun displayText(): String = "$slot: $value"
}

data class LocalVariablesModel(
    val items: List<LocalVariableItem> = emptyList(),
) {
    companion object {
        fun fromFrame(frame: CurrentFrameSnapshot?): LocalVariablesModel =
            LocalVariablesModel(
                frame?.locals.orEmpty().map { local ->
                    LocalVariableItem(
                        slot = local.slot,
                        value = local.value,
                    )
                },
            )
    }
}

object LocalVariablesViewModel {
    const val Title: String = "Local Variables"
}

class LocalVariablesView(
    model: LocalVariablesModel = LocalVariablesModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: LocalVariablesModel) {
        items.setAll(model.items.map(LocalVariableItem::displayText))
    }
}
