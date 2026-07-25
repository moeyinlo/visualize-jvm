package me.moeyinlo.visualize.jvm.nativecall

enum class JvmNativeCallAction {
    Entered,
    Returned,
    Threw,
    FellBackToSimulatedJni,
}

data class JvmNativeCallEventSnapshot(
    val sequence: Long,
    val depth: Int,
    val action: JvmNativeCallAction,
    val signature: JvmNativeMethodSignature,
    val environment: JvmNativeExecutionEnvironment,
    val bindingName: String?,
    val detail: String,
)

fun interface JvmNativeCallEventSink {
    fun record(
        action: JvmNativeCallAction,
        depth: Int,
        frame: JvmNativeMethodFrame,
        detail: String,
    )

    companion object {
        val None: JvmNativeCallEventSink = JvmNativeCallEventSink { _, _, _, _ -> }
    }
}

class JvmNativeCallEventRecorder : JvmNativeCallEventSink {
    private val events = mutableListOf<JvmNativeCallEventSnapshot>()
    private var nextSequence = 1L

    override fun record(
        action: JvmNativeCallAction,
        depth: Int,
        frame: JvmNativeMethodFrame,
        detail: String,
    ) {
        require(depth >= 0) { "native call event depth must be non-negative: $depth" }
        events += JvmNativeCallEventSnapshot(
            sequence = nextSequence,
            depth = depth,
            action = action,
            signature = frame.signature,
            environment = frame.environment,
            bindingName = frame.entryPoint,
            detail = detail,
        )
        nextSequence += 1
    }

    fun snapshots(): List<JvmNativeCallEventSnapshot> = events.toList()
}