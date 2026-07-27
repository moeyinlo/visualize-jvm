package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmVmThreadSetTest {
    @Test
    fun `finishing the last non daemon thread terminates the VM normally`() {
        val threads = JvmVmThreadSet()
        val termination = JvmVmTerminationState()
        threads.startThread("main", isDaemon = false)
        threads.startThread("helper", isDaemon = true)

        assertEquals(null, threads.finishThread("helper", termination))
        assertEquals(false, termination.isTerminated)

        val result = threads.finishThread("main", termination)

        assertEquals(JvmVmTerminationResult.Normal(exitCode = 0), result)
        assertEquals(result, termination.result)
    }

    @Test
    fun `finishing one non daemon thread keeps VM alive while another non daemon thread remains`() {
        val threads = JvmVmThreadSet()
        val termination = JvmVmTerminationState()
        threads.startThread("main", isDaemon = false)
        threads.startThread("worker", isDaemon = false)

        assertEquals(null, threads.finishThread("worker", termination))

        assertEquals(false, termination.isTerminated)
        assertEquals(listOf("main"), threads.activeNonDaemonThreadIds())
    }

    @Test
    fun `thread set rejects duplicate and unknown thread lifecycle transitions`() {
        val threads = JvmVmThreadSet()
        val termination = JvmVmTerminationState()
        threads.startThread("main", isDaemon = false)

        assertFailsWith<JvmVmThreadLifecycleException> {
            threads.startThread("main", isDaemon = true)
        }
        assertFailsWith<JvmVmThreadLifecycleException> {
            threads.finishThread("missing", termination)
        }
    }
}
