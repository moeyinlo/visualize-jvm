package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmThreadSchedulerTest {
    @Test
    fun `scheduler selects the next runnable thread while skipping blocked and waiting threads`() {
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val blockedReference = JvmObjectReferenceValue(JvmReferenceId(1))
        val waitingReference = JvmObjectReferenceValue(JvmReferenceId(2))

        scheduler.tryEnterMonitor(monitors, blockedReference, threadId = "owner")
        scheduler.tryEnterMonitor(monitors, blockedReference, threadId = "blocked")
        scheduler.tryEnterMonitor(monitors, waitingReference, threadId = "waiting")
        scheduler.waitForMonitorNotification(monitors, waitingReference, threadId = "waiting")

        assertEquals(
            listOf("owner", "late"),
            scheduler.runnableThreadIds(listOf("owner", "blocked", "waiting", "late")),
        )
        assertEquals("late", scheduler.nextRunnableThreadId(listOf("owner", "blocked", "waiting", "late"), afterThreadId = "owner"))
        assertEquals("owner", scheduler.nextRunnableThreadId(listOf("owner", "blocked", "waiting", "late"), afterThreadId = "late"))
    }

    @Test
    fun `scheduler reports no next runnable thread when every candidate is blocked or waiting`() {
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val blockedReference = JvmObjectReferenceValue(JvmReferenceId(1))
        val waitingReference = JvmObjectReferenceValue(JvmReferenceId(2))

        scheduler.tryEnterMonitor(monitors, blockedReference, threadId = "owner")
        scheduler.tryEnterMonitor(monitors, blockedReference, threadId = "blocked")
        scheduler.tryEnterMonitor(monitors, waitingReference, threadId = "waiting")
        scheduler.waitForMonitorNotification(monitors, waitingReference, threadId = "waiting")

        assertEquals(emptyList(), scheduler.runnableThreadIds(listOf("blocked", "waiting")))
        assertEquals(null, scheduler.nextRunnableThreadId(listOf("blocked", "waiting"), afterThreadId = "blocked"))
    }

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
    fun `scheduler preserves released wait hold count during notify handoff`() {
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))

        scheduler.tryEnterMonitor(monitors, reference, threadId = "waiter")
        scheduler.tryEnterMonitor(monitors, reference, threadId = "waiter")
        assertEquals(2, scheduler.waitForMonitorNotification(monitors, reference, threadId = "waiter"))

        scheduler.tryEnterMonitor(monitors, reference, threadId = "notifier")
        assertEquals("waiter", scheduler.notifyOneMonitor(monitors, reference, threadId = "notifier"))

        assertEquals(
            JvmThreadSchedulingState.BlockedOnMonitor(
                reference = reference,
                ownerThreadId = "notifier",
                pendingReentryHoldCount = 2,
            ),
            scheduler.state("waiter"),
        )
    }

    @Test
    fun `scheduler restores released wait hold count when notified waiter reacquires monitor`() {
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))

        scheduler.tryEnterMonitor(monitors, reference, threadId = "waiter")
        scheduler.tryEnterMonitor(monitors, reference, threadId = "waiter")
        scheduler.waitForMonitorNotification(monitors, reference, threadId = "waiter")
        scheduler.tryEnterMonitor(monitors, reference, threadId = "notifier")
        scheduler.notifyOneMonitor(monitors, reference, threadId = "notifier")
        scheduler.exitMonitor(monitors, reference, threadId = "notifier")

        assertEquals(
            JvmMonitorEnterResult.Acquired(holdCount = 2),
            scheduler.resumeMonitorReentry(monitors, reference, threadId = "waiter"),
        )
        assertEquals(2, monitors.holdCount(reference, threadId = "waiter"))
        assertEquals(JvmThreadSchedulingState.Runnable, scheduler.state("waiter"))
    }

    @Test
    fun `scheduler keeps pending wait hold count when notified waiter still cannot reacquire monitor`() {
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))

        scheduler.tryEnterMonitor(monitors, reference, threadId = "waiter")
        scheduler.tryEnterMonitor(monitors, reference, threadId = "waiter")
        scheduler.waitForMonitorNotification(monitors, reference, threadId = "waiter")
        scheduler.tryEnterMonitor(monitors, reference, threadId = "notifier")
        scheduler.notifyOneMonitor(monitors, reference, threadId = "notifier")

        assertEquals(
            JvmMonitorEnterResult.Blocked(
                ownerThreadId = "notifier",
                blockedThreadIds = listOf("waiter"),
            ),
            scheduler.resumeMonitorReentry(monitors, reference, threadId = "waiter"),
        )
        assertEquals(
            JvmThreadSchedulingState.BlockedOnMonitor(
                reference = reference,
                ownerThreadId = "notifier",
                pendingReentryHoldCount = 2,
            ),
            scheduler.state("waiter"),
        )
    }

    @Test
    fun `scheduler preserves released wait hold counts during notifyAll handoff`() {
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))

        scheduler.tryEnterMonitor(monitors, reference, threadId = "first")
        scheduler.tryEnterMonitor(monitors, reference, threadId = "first")
        assertEquals(2, scheduler.waitForMonitorNotification(monitors, reference, threadId = "first"))
        scheduler.tryEnterMonitor(monitors, reference, threadId = "second")
        scheduler.tryEnterMonitor(monitors, reference, threadId = "second")
        scheduler.tryEnterMonitor(monitors, reference, threadId = "second")
        assertEquals(3, scheduler.waitForMonitorNotification(monitors, reference, threadId = "second"))

        scheduler.tryEnterMonitor(monitors, reference, threadId = "notifier")
        assertEquals(listOf("first", "second"), scheduler.notifyAllMonitor(monitors, reference, threadId = "notifier"))

        assertEquals(
            JvmThreadSchedulingState.BlockedOnMonitor(
                reference = reference,
                ownerThreadId = "notifier",
                pendingReentryHoldCount = 2,
            ),
            scheduler.state("first"),
        )
        assertEquals(
            JvmThreadSchedulingState.BlockedOnMonitor(
                reference = reference,
                ownerThreadId = "notifier",
                pendingReentryHoldCount = 3,
            ),
            scheduler.state("second"),
        )
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
