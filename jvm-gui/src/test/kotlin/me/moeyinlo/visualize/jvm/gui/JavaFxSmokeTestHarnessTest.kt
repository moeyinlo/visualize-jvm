package me.moeyinlo.visualize.jvm.gui

import javafx.application.Platform
import javafx.scene.layout.VBox
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JavaFxSmokeTestHarnessTest {
    @Test
    fun `harness instantiates root view on the JavaFX application thread`() {
        val rootView = JavaFxSmokeTestHarness.runAndWait {
            assertTrue(Platform.isFxApplicationThread())
            VisualizeJvmRootView()
        }

        assertNotNull(rootView.sceneProperty())
        assertSame(rootView.projectClasspathPanel, rootView.left)
        assertSame(rootView.bytecodeInstructionView, rootView.bottom)

        val top = rootView.top as VBox
        assertSame(rootView.debuggerControlBar, top.children[0])
        assertSame(rootView.constantPoolView, top.children[1])

        val center = rootView.center as VBox
        assertSame(rootView.classTreeView, center.children[0])
        assertSame(rootView.currentFrameView, center.children[1])
        assertSame(rootView.localVariablesView, center.children[2])
        assertSame(rootView.operandStackView, center.children[3])

        val right = rootView.right as VBox
        assertTrue(rootView.nativeIntrinsicFramesView in right.children)
        assertTrue(rootView.simulatedJniCallsView in right.children)
        assertTrue(rootView.jniUpcallNestingView in right.children)
    }
}
