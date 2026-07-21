package me.moeyinlo.visualize.jvm.runtime

class JvmMonitorState {
    private val entries = linkedMapOf<JvmReferenceId, JvmMonitorEntry>()

    fun enter(reference: JvmObjectReferenceValue, threadId: String): Int {
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        val current = entries[reference.referenceId]
        if (current == null) {
            entries[reference.referenceId] = JvmMonitorEntry(ownerThreadId = threadId, holdCount = 1)
            return 1
        }
        if (current.ownerThreadId == null) {
            entries[reference.referenceId] = current.copy(ownerThreadId = threadId, holdCount = 1)
            return 1
        }
        if (current.ownerThreadId != threadId) {
            throw JvmMonitorOwnershipException(
                "Monitor ${reference.referenceId.value} is owned by thread ${current.ownerThreadId} " +
                    "and cannot be entered by thread $threadId",
            )
        }
        val nextCount = current.holdCount + 1
        entries[reference.referenceId] = current.copy(holdCount = nextCount)
        return nextCount
    }

    fun exit(reference: JvmObjectReferenceValue, threadId: String): Int {
        val current = requireOwned(reference, threadId, action = "exited")
        val nextCount = current.holdCount - 1
        if (nextCount == 0) {
            storeOrRemove(reference, current.copy(ownerThreadId = null, holdCount = 0))
        } else {
            entries[reference.referenceId] = current.copy(holdCount = nextCount)
        }
        return nextCount
    }

    fun holdCount(reference: JvmObjectReferenceValue, threadId: String): Int {
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        val current = entries[reference.referenceId] ?: return 0
        return if (current.ownerThreadId == threadId) current.holdCount else 0
    }

    fun waitForNotification(reference: JvmObjectReferenceValue, threadId: String): Int {
        val current = requireOwned(reference, threadId, action = "waited on")
        val waitingThreadIds = current.waitingThreadIds.copyToLinkedSet()
        waitingThreadIds.add(threadId)
        entries[reference.referenceId] = current.copy(
            ownerThreadId = null,
            holdCount = 0,
            waitingThreadIds = waitingThreadIds,
        )
        return current.holdCount
    }

    fun notifyOne(reference: JvmObjectReferenceValue, threadId: String): String? {
        val current = requireOwned(reference, threadId, action = "notified")
        val waitingThreadIds = current.waitingThreadIds.copyToLinkedSet()
        val notifiedThreadId = waitingThreadIds.firstOrNull()
        if (notifiedThreadId != null) {
            waitingThreadIds.remove(notifiedThreadId)
        }
        entries[reference.referenceId] = current.copy(waitingThreadIds = waitingThreadIds)
        return notifiedThreadId
    }

    fun notifyAll(reference: JvmObjectReferenceValue, threadId: String): List<String> {
        val current = requireOwned(reference, threadId, action = "notified")
        val notifiedThreadIds = current.waitingThreadIds.toList()
        entries[reference.referenceId] = current.copy(waitingThreadIds = linkedSetOf())
        return notifiedThreadIds
    }

    fun waitingThreads(reference: JvmObjectReferenceValue): List<String> =
        entries[reference.referenceId]?.waitingThreadIds?.toList().orEmpty()

    private fun requireOwned(
        reference: JvmObjectReferenceValue,
        threadId: String,
        action: String,
    ): JvmMonitorEntry {
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        val current = entries[reference.referenceId]
            ?: throw JvmMonitorOwnershipException(
                "Monitor ${reference.referenceId.value} is not owned by thread $threadId",
            )
        val ownerThreadId = current.ownerThreadId
            ?: throw JvmMonitorOwnershipException(
                "Monitor ${reference.referenceId.value} is not owned by thread $threadId",
            )
        if (ownerThreadId != threadId) {
            throw JvmMonitorOwnershipException(
                "Monitor ${reference.referenceId.value} is owned by thread $ownerThreadId " +
                    "and cannot be $action by thread $threadId",
            )
        }
        return current
    }

    private fun storeOrRemove(reference: JvmObjectReferenceValue, entry: JvmMonitorEntry) {
        if (entry.ownerThreadId == null && entry.waitingThreadIds.isEmpty()) {
            entries.remove(reference.referenceId)
        } else {
            entries[reference.referenceId] = entry
        }
    }
}

private data class JvmMonitorEntry(
    val ownerThreadId: String?,
    val holdCount: Int,
    val waitingThreadIds: Set<String> = linkedSetOf(),
)

class JvmMonitorOwnershipException(message: String) : IllegalStateException(message)

private fun Set<String>.copyToLinkedSet(): LinkedHashSet<String> =
    linkedSetOf<String>().also { copy -> copy.addAll(this) }
