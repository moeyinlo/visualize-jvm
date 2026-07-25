package me.moeyinlo.visualize.jvm.host

enum class JvmHostBoundaryAction {
    Delegated,
    Returned,
    Failed,
}

data class JvmHostBoundaryEventSnapshot(
    val sequence: Long,
    val action: JvmHostBoundaryAction,
    val className: String,
    val methodName: String,
    val descriptor: String,
    val detail: String,
)

fun interface JvmHostBoundaryEventSink {
    fun record(
        action: JvmHostBoundaryAction,
        className: String,
        methodName: String,
        descriptor: String,
        detail: String,
    )

    companion object {
        val None: JvmHostBoundaryEventSink = JvmHostBoundaryEventSink { _, _, _, _, _ -> }
    }
}

class JvmHostBoundaryEventRecorder : JvmHostBoundaryEventSink {
    private val events = mutableListOf<JvmHostBoundaryEventSnapshot>()
    private var nextSequence = 1L

    override fun record(
        action: JvmHostBoundaryAction,
        className: String,
        methodName: String,
        descriptor: String,
        detail: String,
    ) {
        events += JvmHostBoundaryEventSnapshot(
            sequence = nextSequence,
            action = action,
            className = className,
            methodName = methodName,
            descriptor = descriptor,
            detail = detail,
        )
        nextSequence += 1
    }

    fun snapshots(): List<JvmHostBoundaryEventSnapshot> = events.toList()
}
