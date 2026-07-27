package me.moeyinlo.visualize.jvm.runtime

class JvmThreadScheduler {
    private val statesByThreadId = linkedMapOf<String, JvmThreadSchedulingState>()
    private val pendingReentryHoldCountsByThreadId = linkedMapOf<String, Int>()
    private val pendingReentryReferencesByThreadId = linkedMapOf<String, JvmObjectReferenceValue>()

    fun state(threadId: String): JvmThreadSchedulingState {
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        return statesByThreadId[threadId] ?: JvmThreadSchedulingState.Runnable
    }

    fun runnableThreadIds(threadIds: Iterable<String>): List<String> =
        threadIds
            .map { threadId ->
                require(threadId.isNotBlank()) { "thread id must not be blank" }
                threadId
            }
            .filter { threadId -> state(threadId) == JvmThreadSchedulingState.Runnable }

    fun nextRunnableThreadId(
        threadIds: List<String>,
        afterThreadId: String? = null,
    ): String? {
        if (threadIds.isEmpty()) {
            return null
        }
        threadIds.forEach { threadId ->
            require(threadId.isNotBlank()) { "thread id must not be blank" }
        }
        afterThreadId?.let { threadId ->
            require(threadId.isNotBlank()) { "thread id must not be blank" }
        }

        val startIndex = afterThreadId
            ?.let(threadIds::indexOf)
            ?.takeIf { index -> index >= 0 }
            ?.let { index -> (index + 1) % threadIds.size }
            ?: 0
        for (offset in threadIds.indices) {
            val candidate = threadIds[(startIndex + offset) % threadIds.size]
            if (state(candidate) == JvmThreadSchedulingState.Runnable) {
                return candidate
            }
        }
        return null
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
            pendingReentryReferencesByThreadId[notifiedThreadId] = reference
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
            pendingReentryReferencesByThreadId[notifiedThreadId] = reference
            val result = monitors.tryEnter(reference, notifiedThreadId)
            recordMonitorEnterResult(reference, notifiedThreadId, result, pendingReentryHoldCount)
        }
        return notifiedThreadIds
    }

    fun resumePendingMonitorReentry(
        monitors: JvmMonitorState,
        threadId: String,
    ): JvmMonitorEnterResult? {
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        val reference = pendingReentryReferencesByThreadId[threadId]
            ?: (statesByThreadId[threadId] as? JvmThreadSchedulingState.BlockedOnMonitor)?.reference
            ?: return null
        return resumeMonitorReentry(monitors, reference, threadId)
    }

    fun resumeClassInitializationWaiters(threadIds: Iterable<String>): List<String> {
        val resumedThreadIds = linkedSetOf<String>()
        threadIds.forEach { threadId ->
            require(threadId.isNotBlank()) { "thread id must not be blank" }
            resumedThreadIds.add(threadId)
        }
        resumedThreadIds.forEach { threadId ->
            pendingReentryHoldCountsByThreadId.remove(threadId)
            pendingReentryReferencesByThreadId.remove(threadId)
            statesByThreadId[threadId] = JvmThreadSchedulingState.Runnable
        }
        return resumedThreadIds.toList()
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
                pendingReentryReferencesByThreadId.remove(threadId)
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
