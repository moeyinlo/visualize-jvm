package me.moeyinlo.visualize.jvm.nativecall

data class JvmNativeMethodFrame(
    val ownerClassName: String,
    val methodName: String,
    val methodDescriptor: String,
    val isStatic: Boolean,
    val libraryName: String? = null,
    val entryPoint: String? = null,
    val environment: JvmNativeExecutionEnvironment = JvmNativeExecutionEnvironment.SimulatedJni,
) {
    init {
        require(ownerClassName.isNotBlank()) { "native frame owner class name must not be blank" }
        require(methodName.isNotBlank()) { "native frame method name must not be blank" }
        require(methodDescriptor.isNotBlank()) { "native frame method descriptor must not be blank" }
        require(libraryName == null || libraryName.isNotBlank()) { "native frame library name must not be blank" }
        require(entryPoint == null || entryPoint.isNotBlank()) { "native frame entry point must not be blank" }
    }
}

enum class JvmNativeExecutionEnvironment {
    VmIntrinsic,
    SimulatedJni,
    HostDowncall,
}

class JvmNativeMethodStack(maxFrames: Int? = null) {
    init {
        require(maxFrames == null || maxFrames >= 0) {
            "max native frame count must be non-negative: $maxFrames"
        }
    }

    private val frames = mutableListOf<JvmNativeMethodFrame>()
    private val maxFrameCount = maxFrames

    val depth: Int
        get() = frames.size

    fun push(frame: JvmNativeMethodFrame) {
        if (maxFrameCount != null && frames.size >= maxFrameCount) {
            throw JvmNativeStackOverflowException(
                "Native method stack depth ${frames.size + 1} exceeds max_native_frames=$maxFrameCount",
            )
        }

        frames.add(frame)
    }

    fun pop(): JvmNativeMethodFrame =
        frames.removeLastOrNull()
            ?: throw JvmNativeStackUnderflowException("Native method stack is empty")

    fun currentFrame(): JvmNativeMethodFrame =
        frames.lastOrNull()
            ?: throw JvmNativeStackUnderflowException("Native method stack is empty")

    fun toList(): List<JvmNativeMethodFrame> = frames.toList()
}

class JvmNativeStackOverflowException(message: String) : IllegalStateException(message)

class JvmNativeStackUnderflowException(message: String) : IllegalStateException(message)
