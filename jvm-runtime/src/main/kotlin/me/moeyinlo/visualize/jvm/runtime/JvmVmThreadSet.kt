package me.moeyinlo.visualize.jvm.runtime

class JvmVmThreadSet {
    private val threadsById = linkedMapOf<String, JvmVmThreadRecord>()

    fun startThread(threadId: String, isDaemon: Boolean) {
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        if (threadId in threadsById) {
            throw JvmVmThreadLifecycleException("VM thread $threadId is already active")
        }
        threadsById[threadId] = JvmVmThreadRecord(isDaemon)
    }

    fun finishThread(
        threadId: String,
        termination: JvmVmTerminationState,
    ): JvmVmTerminationResult? {
        val thread = removeActiveThread(threadId)
        return if (!thread.isDaemon && activeNonDaemonThreadIds().isEmpty()) {
            termination.terminateNormally(exitCode = 0)
        } else {
            null
        }
    }

    fun finishThreadAbruptly(
        threadId: String,
        throwable: JvmObjectReferenceValue,
        termination: JvmVmTerminationState,
    ): JvmVmTerminationResult? {
        val thread = removeActiveThread(threadId)
        return if (!thread.isDaemon && activeNonDaemonThreadIds().isEmpty()) {
            termination.terminateAbruptly(throwable)
        } else {
            null
        }
    }

    fun terminateIfNoActiveNonDaemonThreads(termination: JvmVmTerminationState): JvmVmTerminationResult? =
        termination.result ?: if (activeNonDaemonThreadIds().isEmpty()) {
            termination.terminateNormally(exitCode = 0)
        } else {
            null
        }

    fun activeNonDaemonThreadIds(): List<String> =
        threadsById
            .filterValues { thread -> !thread.isDaemon }
            .keys
            .toList()

    private fun removeActiveThread(threadId: String): JvmVmThreadRecord {
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        return threadsById.remove(threadId)
            ?: throw JvmVmThreadLifecycleException("VM thread $threadId is not active")
    }
}

private data class JvmVmThreadRecord(
    val isDaemon: Boolean,
)

class JvmVmThreadLifecycleException(message: String) : IllegalStateException(message)
