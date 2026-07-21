package me.moeyinlo.visualize.jvm.gui

import kotlin.test.Test
import kotlin.test.assertEquals

class DebuggerStepControllerTest {
    @Test
    fun `step action invokes debugger engine with current run configuration`() {
        val configuration = RunConfiguration(
            mainClassName = "demo/Main",
            programArguments = listOf("arg"),
        )
        val engine = RecordingDebuggerEngine()
        val controller = DebuggerStepController(
            runConfiguration = configuration,
            engine = engine,
        )

        val result = controller.handle(DebuggerControlAction.Step)

        assertEquals(DebuggerStepResult(stepIndex = 1), result)
        assertEquals(listOf(configuration), engine.stepConfigurations)
    }

    @Test
    fun `non step debugger actions do not invoke debugger engine`() {
        val engine = RecordingDebuggerEngine()
        val controller = DebuggerStepController(
            runConfiguration = RunConfiguration(mainClassName = "demo/Main"),
            engine = engine,
        )

        val result = controller.handle(DebuggerControlAction.Run)

        assertEquals(null, result)
        assertEquals(emptyList(), engine.stepConfigurations)
    }

    private class RecordingDebuggerEngine : DebuggerStepEngine {
        val stepConfigurations: MutableList<RunConfiguration> = mutableListOf()

        override fun step(configuration: RunConfiguration): DebuggerStepResult {
            stepConfigurations += configuration
            return DebuggerStepResult(stepIndex = stepConfigurations.size)
        }
    }
}
