package me.moeyinlo.visualize.jvm.verifier

data class JvmVerificationInput(
    val majorVersion: Int,
    val hasStackMapTable: Boolean,
) {
    init {
        require(majorVersion > 0) { "classfile major_version must be positive: $majorVersion" }
    }
}

enum class JvmVerificationStrategy {
    TypeChecking,
    TypeCheckingWithInferredFrames,
    TypeInference,
}

object JvmVerificationStrategySelector {
    private const val Java6MajorVersion = 50

    fun select(input: JvmVerificationInput): JvmVerificationStrategy =
        when {
            input.majorVersion < Java6MajorVersion -> JvmVerificationStrategy.TypeInference
            input.hasStackMapTable -> JvmVerificationStrategy.TypeChecking
            else -> JvmVerificationStrategy.TypeCheckingWithInferredFrames
        }
}
