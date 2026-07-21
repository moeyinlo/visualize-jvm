package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView

enum class VerifierDiagnosticSeverity {
    Info,
    Warning,
    Error,
}

data class VerifierDiagnosticSnapshot(
    val sequence: Long,
    val severity: VerifierDiagnosticSeverity,
    val className: String,
    val methodName: String,
    val descriptor: String,
    val bytecodeOffset: Int,
    val message: String,
)

data class VerifierDiagnosticItem(
    val sequence: Long,
    val text: String,
)

data class VerifierDiagnosticsModel(
    val items: List<VerifierDiagnosticItem> = emptyList(),
) {
    companion object {
        fun fromDiagnostics(diagnostics: List<VerifierDiagnosticSnapshot>): VerifierDiagnosticsModel =
            VerifierDiagnosticsModel(
                diagnostics.map { diagnostic ->
                    VerifierDiagnosticItem(
                        sequence = diagnostic.sequence,
                        text = "#${diagnostic.sequence} ${diagnostic.severity.name.uppercase()} " +
                            "${diagnostic.className}.${diagnostic.methodName}${diagnostic.descriptor} " +
                            "@ bci=${diagnostic.bytecodeOffset}: ${diagnostic.message}",
                    )
                },
            )
    }
}

object VerifierDiagnosticsViewModel {
    const val Title: String = "Verifier Diagnostics"
}

class VerifierDiagnosticsView(
    model: VerifierDiagnosticsModel = VerifierDiagnosticsModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: VerifierDiagnosticsModel) {
        items.setAll(model.items.map(VerifierDiagnosticItem::text))
    }
}
