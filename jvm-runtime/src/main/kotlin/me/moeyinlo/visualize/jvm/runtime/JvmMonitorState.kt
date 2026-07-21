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
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        val current = entries[reference.referenceId]
            ?: throw JvmMonitorOwnershipException(
                "Monitor ${reference.referenceId.value} is not owned by thread $threadId",
            )
        if (current.ownerThreadId != threadId) {
            throw JvmMonitorOwnershipException(
                "Monitor ${reference.referenceId.value} is owned by thread ${current.ownerThreadId} " +
                    "and cannot be exited by thread $threadId",
            )
        }
        val nextCount = current.holdCount - 1
        if (nextCount == 0) {
            entries.remove(reference.referenceId)
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
}

private data class JvmMonitorEntry(
    val ownerThreadId: String,
    val holdCount: Int,
)

class JvmMonitorOwnershipException(message: String) : IllegalStateException(message)
