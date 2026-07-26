package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmThreadSchedulerTest {
    @Test
    fun `scheduler parks monitor contender and resumes it when owner releases`() {
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))

        assertEquals(
            JvmMonitorEnterResult.Acquired(holdCount = 1),
            scheduler.tryEnterMonitor(monitors, reference, threadId = "owner"),
        )
        assertEquals(
            JvmMonitorEnterResult.Blocked(
                ownerThreadId = "owner",
                blockedThreadIds = listOf("contender"),
            ),
            scheduler.tryEnterMonitor(monitors, reference, threadId = "contender"),
        )
        assertEquals(
            JvmThreadSchedulingState.BlockedOnMonitor(
                reference = reference,
                ownerThreadId = "owner",
            ),
            scheduler.state("contender"),
        )

        assertEquals(
            JvmMonitorExitResult(holdCount = 0, unblockedThreadId = "contender"),
            scheduler.exitMonitor(monitors, reference, threadId = "owner"),
        )
        assertEquals(JvmThreadSchedulingState.Runnable, scheduler.state("contender"))
        assertEquals(
            JvmMonitorEnterResult.Acquired(holdCount = 1),
            scheduler.tryEnterMonitor(monitors, reference, threadId = "contender"),
        )
    }
    @Test
    fun `scheduler moves notified waiter into monitor blocked handoff before resume`() {
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))

        scheduler.tryEnterMonitor(monitors, reference, threadId = "waiter")
        assertEquals(1, scheduler.waitForMonitorNotification(monitors, reference, threadId = "waiter"))
        assertEquals(
            JvmThreadSchedulingState.WaitingOnMonitor(
                reference = reference,
                releasedHoldCount = 1,
            ),
            scheduler.state("waiter"),
        )

        scheduler.tryEnterMonitor(monitors, reference, threadId = "notifier")
        assertEquals("waiter", scheduler.notifyOneMonitor(monitors, reference, threadId = "notifier"))
        assertEquals(
            JvmThreadSchedulingState.BlockedOnMonitor(
                reference = reference,
                ownerThreadId = "notifier",
            ),
            scheduler.state("waiter"),
        )
        assertEquals(listOf("waiter"), monitors.blockedThreads(reference))

        assertEquals(
            JvmMonitorExitResult(holdCount = 0, unblockedThreadId = "waiter"),
            scheduler.exitMonitor(monitors, reference, threadId = "notifier"),
        )
        assertEquals(JvmThreadSchedulingState.Runnable, scheduler.state("waiter"))
    }

    @Test
    fun `scheduler moves all notified waiters into monitor blocked handoff before resume`() {
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))

        scheduler.tryEnterMonitor(monitors, reference, threadId = "first")
        assertEquals(1, scheduler.waitForMonitorNotification(monitors, reference, threadId = "first"))
        scheduler.tryEnterMonitor(monitors, reference, threadId = "second")
        assertEquals(1, scheduler.waitForMonitorNotification(monitors, reference, threadId = "second"))

        scheduler.tryEnterMonitor(monitors, reference, threadId = "notifier")
        assertEquals(
            listOf("first", "second"),
            scheduler.notifyAllMonitor(monitors, reference, threadId = "notifier"),
        )
        assertEquals(
            JvmThreadSchedulingState.BlockedOnMonitor(
                reference = reference,
                ownerThreadId = "notifier",
            ),
            scheduler.state("first"),
        )
        assertEquals(
            JvmThreadSchedulingState.BlockedOnMonitor(
                reference = reference,
                ownerThreadId = "notifier",
            ),
            scheduler.state("second"),
        )
        assertEquals(listOf("first", "second"), monitors.blockedThreads(reference))
    }
}
