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
}
