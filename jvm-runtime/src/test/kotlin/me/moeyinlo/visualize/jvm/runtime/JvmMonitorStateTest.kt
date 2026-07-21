package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmMonitorStateTest {
    @Test
    fun `monitor enter records reentrant ownership per object and thread`() {
        val monitors = JvmMonitorState()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))

        assertEquals(1, monitors.enter(reference, threadId = "main"))
        assertEquals(2, monitors.enter(reference, threadId = "main"))

        assertEquals(2, monitors.holdCount(reference, threadId = "main"))
    }

    @Test
    fun `monitor enter rejects objects owned by another thread`() {
        val monitors = JvmMonitorState()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        monitors.enter(reference, threadId = "owner")

        val exception = assertFailsWith<JvmMonitorOwnershipException> {
            monitors.enter(reference, threadId = "contender")
        }

        assertEquals(
            "Monitor 1 is owned by thread owner and cannot be entered by thread contender",
            exception.message,
        )
    }

    @Test
    fun `monitor exit decrements ownership and rejects non owners`() {
        val monitors = JvmMonitorState()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        monitors.enter(reference, threadId = "main")
        monitors.enter(reference, threadId = "main")

        assertEquals(1, monitors.exit(reference, threadId = "main"))
        assertEquals(0, monitors.exit(reference, threadId = "main"))

        assertFailsWith<JvmMonitorOwnershipException> {
            monitors.exit(reference, threadId = "main")
        }
    }

    @Test
    fun `waitForNotification releases ownership and records waiting threads`() {
        val monitors = JvmMonitorState()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        monitors.enter(reference, threadId = "main")
        monitors.enter(reference, threadId = "main")

        val releasedHoldCount = monitors.waitForNotification(reference, threadId = "main")

        assertEquals(2, releasedHoldCount)
        assertEquals(0, monitors.holdCount(reference, threadId = "main"))
        assertEquals(listOf("main"), monitors.waitingThreads(reference))
        assertEquals(1, monitors.enter(reference, threadId = "other"))
    }

    @Test
    fun `notifyOne requires ownership and removes the first waiting thread`() {
        val monitors = JvmMonitorState()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        monitors.enter(reference, threadId = "first")
        monitors.waitForNotification(reference, threadId = "first")
        monitors.enter(reference, threadId = "second")
        monitors.waitForNotification(reference, threadId = "second")

        assertFailsWith<JvmMonitorOwnershipException> {
            monitors.notifyOne(reference, threadId = "not-owner")
        }

        monitors.enter(reference, threadId = "owner")

        assertEquals("first", monitors.notifyOne(reference, threadId = "owner"))
        assertEquals(listOf("second"), monitors.waitingThreads(reference))
    }

    @Test
    fun `notifyAll requires ownership and clears every waiting thread`() {
        val monitors = JvmMonitorState()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        monitors.enter(reference, threadId = "first")
        monitors.waitForNotification(reference, threadId = "first")
        monitors.enter(reference, threadId = "second")
        monitors.waitForNotification(reference, threadId = "second")

        assertFailsWith<JvmMonitorOwnershipException> {
            monitors.notifyAll(reference, threadId = "not-owner")
        }

        monitors.enter(reference, threadId = "owner")

        assertEquals(listOf("first", "second"), monitors.notifyAll(reference, threadId = "owner"))
        assertEquals(emptyList(), monitors.waitingThreads(reference))
    }
}
