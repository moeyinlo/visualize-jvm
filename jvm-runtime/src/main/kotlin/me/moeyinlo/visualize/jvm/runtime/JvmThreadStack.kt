package me.moeyinlo.visualize.jvm.runtime

interface JvmFrame

class JvmThreadStack(maxFrames: Int? = null) {
    init {
        require(maxFrames == null || maxFrames >= 0) {
            "max frame count must be non-negative: $maxFrames"
        }
    }

    private val frames = mutableListOf<JvmFrame>()
    private val maxFrameCount = maxFrames

    val depth: Int
        get() = frames.size

    fun push(frame: JvmFrame) {
        if (maxFrameCount != null && frames.size >= maxFrameCount) {
            throw JvmStackOverflowException(
                "JVM stack depth ${frames.size + 1} exceeds max_frames=$maxFrameCount",
            )
        }

        frames.add(frame)
    }

    fun pop(): JvmFrame =
        frames.removeLastOrNull()
            ?: throw JvmStackUnderflowException("JVM stack is empty")

    fun currentFrame(): JvmFrame =
        frames.lastOrNull()
            ?: throw JvmStackUnderflowException("JVM stack is empty")

    fun toList(): List<JvmFrame> = frames.toList()
}

class JvmStackOverflowException(message: String) : IllegalStateException(message)

class JvmStackUnderflowException(message: String) : IllegalStateException(message)
