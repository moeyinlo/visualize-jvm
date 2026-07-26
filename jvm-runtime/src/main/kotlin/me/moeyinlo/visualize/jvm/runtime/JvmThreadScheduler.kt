package me.moeyinlo.visualize.jvm.runtime

class JvmThreadScheduler {
    private val statesByThreadId = linkedMapOf<String, JvmThreadSchedulingState>()

    fun state(threadId: String): JvmThreadSchedulingState {
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        return statesByThreadId[threadId] ?: JvmThreadSchedulingState.Runnable
    }

    fun tryEnterMonitor(
        monitors: JvmMonitorState,
        reference: JvmObjectReferenceValue,
        threadId: String,
    ): JvmMonitorEnterResult {
        return when (val result = monitors.tryEnter(reference, threadId)) {
            is JvmMonitorEnterResult.Acquired -> {
                statesByThreadId[threadId] = JvmThreadSchedulingState.Runnable
                result
            }
            is JvmMonitorEnterResult.Blocked -> {
                statesByThreadId[threadId] = JvmThreadSchedulingState.BlockedOnMonitor(
                    reference = reference,
                    ownerThreadId = result.ownerThreadId,
                )
                result
            }
        }
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
}

sealed interface JvmThreadSchedulingState {
    data object Runnable : JvmThreadSchedulingState

    data class BlockedOnMonitor(
        val reference: JvmObjectReferenceValue,
        val ownerThreadId: String,
    ) : JvmThreadSchedulingState {
        init {
            require(ownerThreadId.isNotBlank()) { "owner thread id must not be blank" }
        }
    }
}