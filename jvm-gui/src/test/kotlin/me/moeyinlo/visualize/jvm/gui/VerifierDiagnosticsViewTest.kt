package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerifierDiagnosticsViewTest {
    @Test
    fun `verifier diagnostics model formats diagnostic location and message`() {
        val model = VerifierDiagnosticsModel.fromDiagnostics(
            listOf(
                VerifierDiagnosticSnapshot(
                    sequence = 8,
                    severity = VerifierDiagnosticSeverity.Error,
                    className = "demo/Main",
                    methodName = "bad",
                    descriptor = "()V",
                    bytecodeOffset = 12,
                    message = "Operand stack is empty",
                ),
            ),
        )

        assertEquals(
            listOf(
                VerifierDiagnosticItem(
                    sequence = 8,
                    text = "#8 ERROR demo/Main.bad()V @ bci=12: Operand stack is empty",
                ),
            ),
            model.items,
        )
    }

    @Test
    fun `verifier diagnostics model preserves empty diagnostics`() {
        assertEquals(emptyList(), VerifierDiagnosticsModel.fromDiagnostics(emptyList()).items)
    }

    @Test
    fun `verifier diagnostics view is exposed as a JavaFX list view type`() {
        assertEquals("Verifier Diagnostics", VerifierDiagnosticsViewModel.Title)
        assertTrue(ListView::class.java.isAssignableFrom(VerifierDiagnosticsView::class.java))
    }
}
