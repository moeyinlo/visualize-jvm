package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmVmTerminationStateTest {
    @Test
    fun `fresh VM termination state starts unterminated`() {
        val termination = JvmVmTerminationState()

        assertEquals(false, termination.isTerminated)
        assertEquals(null, termination.result)
    }

    @Test
    fun `VM termination state records normal exit status`() {
        val termination = JvmVmTerminationState()

        val result = termination.terminateNormally(exitCode = 7)

        assertEquals(JvmVmTerminationResult.Normal(exitCode = 7), result)
        assertEquals(true, termination.isTerminated)
        assertEquals(result, termination.result)
    }

    @Test
    fun `VM termination state records uncaught guest exceptions`() {
        val termination = JvmVmTerminationState()
        val throwable = JvmObjectReferenceValue(JvmReferenceId(42))

        val result = termination.terminateAbruptly(throwable)

        assertEquals(JvmVmTerminationResult.UncaughtGuestException(throwable), result)
        assertEquals(true, termination.isTerminated)
        assertEquals(result, termination.result)
    }

    @Test
    fun `VM termination state rejects duplicate termination`() {
        val termination = JvmVmTerminationState()
        termination.terminateNormally(exitCode = 0)

        val exception = assertFailsWith<JvmVmTerminationException> {
            termination.terminateAbruptly(JvmObjectReferenceValue(JvmReferenceId(1)))
        }

        assertEquals("VM already terminated with Normal(exitCode=0)", exception.message)
        assertEquals(JvmVmTerminationResult.Normal(exitCode = 0), termination.result)
    }
}
