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
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        val thread = threadsById.remove(threadId)
            ?: throw JvmVmThreadLifecycleException("VM thread $threadId is not active")
        return if (!thread.isDaemon && activeNonDaemonThreadIds().isEmpty()) {
            termination.terminateNormally(exitCode = 0)
        } else {
            null
        }
    }

    fun activeNonDaemonThreadIds(): List<String> =
        threadsById
            .filterValues { thread -> !thread.isDaemon }
            .keys
            .toList()
}

private data class JvmVmThreadRecord(
    val isDaemon: Boolean,
)

class JvmVmThreadLifecycleException(message: String) : IllegalStateException(message)
