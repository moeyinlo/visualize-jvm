package me.moeyinlo.visualize.jvm.runtime

class JvmThreadScheduler {
    private val statesByThreadId = linkedMapOf<String, JvmThreadSchedulingState>()
    private val pendingReentryHoldCountsByThreadId = linkedMapOf<String, Int>()

    fun state(threadId: String): JvmThreadSchedulingState {
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        return statesByThreadId[threadId] ?: JvmThreadSchedulingState.Runnable
    }

    fun tryEnterMonitor(
        monitors: JvmMonitorState,
        reference: JvmObjectReferenceValue,
        threadId: String,
    ): JvmMonitorEnterResult {
        val result = monitors.tryEnter(reference, threadId)
        recordMonitorEnterResult(reference, threadId, result)
        return result
    }

    fun exitMonitor(
        monitors: JvmMonitorState,
        reference: JvmObjectReferenceValue,
        threadId: String,
    ): JvmMonitorExitResult {
        val result = monitors.exitAndSelectUnblocked(reference, threadId)
        statesByThreadId[threadId] = JvmThreadSchedulingState.Runnable
        result.unblockedThreadId?.let { unblockedThreadId ->
            statesByThreadId[unblockedThreadId] = JvmThreadSchedulingState.Runnable
        }
        return result
    }

    fun waitForMonitorNotification(
        monitors: JvmMonitorState,
        reference: JvmObjectReferenceValue,
        threadId: String,
    ): Int {
        val releasedHoldCount = monitors.waitForNotification(reference, threadId)
        statesByThreadId[threadId] = JvmThreadSchedulingState.WaitingOnMonitor(
            reference = reference,
            releasedHoldCount = releasedHoldCount,
        )
        return releasedHoldCount
    }

    fun notifyOneMonitor(
        monitors: JvmMonitorState,
        reference: JvmObjectReferenceValue,
        threadId: String,
    ): String? {
        val notifiedThreadId = monitors.notifyOne(reference, threadId)
        if (notifiedThreadId != null) {
            val pendingReentryHoldCount = waitReleasedHoldCount(notifiedThreadId)
            pendingReentryHoldCountsByThreadId[notifiedThreadId] = pendingReentryHoldCount
            val result = monitors.tryEnter(reference, notifiedThreadId)
            recordMonitorEnterResult(reference, notifiedThreadId, result, pendingReentryHoldCount)
        }
        return notifiedThreadId
    }

    fun notifyAllMonitor(
        monitors: JvmMonitorState,
        reference: JvmObjectReferenceValue,
        threadId: String,
    ): List<String> {
        val notifiedThreadIds = monitors.notifyAll(reference, threadId)
        for (notifiedThreadId in notifiedThreadIds) {
            val pendingReentryHoldCount = waitReleasedHoldCount(notifiedThreadId)
            pendingReentryHoldCountsByThreadId[notifiedThreadId] = pendingReentryHoldCount
            val result = monitors.tryEnter(reference, notifiedThreadId)
            recordMonitorEnterResult(reference, notifiedThreadId, result, pendingReentryHoldCount)
        }
        return notifiedThreadIds
    }

    fun resumeMonitorReentry(
        monitors: JvmMonitorState,
        reference: JvmObjectReferenceValue,
        threadId: String,
    ): JvmMonitorEnterResult {
        val pendingReentryHoldCount = pendingReentryHoldCountsByThreadId[threadId]
            ?: ((statesByThreadId[threadId] as? JvmThreadSchedulingState.BlockedOnMonitor)
                ?.takeIf { state -> state.reference == reference }
                ?.pendingReentryHoldCount ?: 1)
        val result = monitors.tryEnter(reference, threadId)
        when (result) {
            is JvmMonitorEnterResult.Acquired -> {
                for (reentry in 2..pendingReentryHoldCount) {
                    monitors.enter(reference, threadId)
                }
                pendingReentryHoldCountsByThreadId.remove(threadId)
                statesByThreadId[threadId] = JvmThreadSchedulingState.Runnable
                return JvmMonitorEnterResult.Acquired(holdCount = pendingReentryHoldCount)
            }
            is JvmMonitorEnterResult.Blocked -> {
                recordMonitorEnterResult(reference, threadId, result, pendingReentryHoldCount)
                return result
            }
        }
    }

    private fun waitReleasedHoldCount(threadId: String): Int =
        (statesByThreadId[threadId] as? JvmThreadSchedulingState.WaitingOnMonitor)?.releasedHoldCount ?: 1

    private fun recordMonitorEnterResult(
        reference: JvmObjectReferenceValue,
        threadId: String,
        result: JvmMonitorEnterResult,
        pendingReentryHoldCount: Int = 1,
    ) {
        when (result) {
            is JvmMonitorEnterResult.Acquired -> {
                statesByThreadId[threadId] = JvmThreadSchedulingState.Runnable
            }
            is JvmMonitorEnterResult.Blocked -> {
                statesByThreadId[threadId] = JvmThreadSchedulingState.BlockedOnMonitor(
                    reference = reference,
                    ownerThreadId = result.ownerThreadId,
                    pendingReentryHoldCount = pendingReentryHoldCount,
                )
            }
        }
    }
}

sealed interface JvmThreadSchedulingState {
    data object Runnable : JvmThreadSchedulingState

    data class BlockedOnMonitor(
        val reference: JvmObjectReferenceValue,
        val ownerThreadId: String,
        val pendingReentryHoldCount: Int = 1,
    ) : JvmThreadSchedulingState {
        init {
            require(ownerThreadId.isNotBlank()) { "owner thread id must not be blank" }
            require(pendingReentryHoldCount > 0) {
                "pending reentry hold count must be positive: $pendingReentryHoldCount"
            }
        }
    }

    data class WaitingOnMonitor(
        val reference: JvmObjectReferenceValue,
        val releasedHoldCount: Int,
    ) : JvmThreadSchedulingState {
        init {
            require(releasedHoldCount > 0) { "released hold count must be positive: $releasedHoldCount" }
        }
    }
}
