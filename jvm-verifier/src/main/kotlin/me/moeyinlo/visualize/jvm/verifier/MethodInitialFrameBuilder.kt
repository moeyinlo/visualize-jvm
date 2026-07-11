package me.moeyinlo.visualize.jvm.verifier

data class MethodInitialFrame(
    val locals: List<VerificationType>,
    val stack: List<VerificationType>,
    val flags: List<MethodInitialFrameFlag>,
    val returnType: VerificationType?,
)

enum class MethodInitialFrameFlag {
    ThisUninitialized,
}

object MethodInitialFrameBuilder {
    fun buildStatic(
        descriptor: String,
        maxLocals: Int,
    ): MethodInitialFrame {
        val descriptorTypes = MethodDescriptorVerificationTypeParser.parse(descriptor)
        val argumentLocals = VerificationTypeSlotExpander.expand(descriptorTypes.parameterTypes)
        if (argumentLocals.size > maxLocals) {
            throw MethodVerificationException(
                "Initial frame locals use ${argumentLocals.size} local variable unit(s), exceeding max_locals=$maxLocals",
            )
        }
        val paddedLocals = argumentLocals + List(maxLocals - argumentLocals.size) { VerificationType.Top }
        return MethodInitialFrame(
            locals = paddedLocals,
            stack = emptyList(),
            flags = emptyList(),
            returnType = descriptorTypes.returnType,
        )
    }
}
