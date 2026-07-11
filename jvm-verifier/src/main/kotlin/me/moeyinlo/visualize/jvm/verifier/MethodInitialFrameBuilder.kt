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
    ): MethodInitialFrame =
        build(
            descriptor = descriptor,
            maxLocals = maxLocals,
            thisLocals = emptyList(),
            flags = emptyList(),
        )

    fun buildInstance(
        currentClass: String,
        currentClassLoader: String = "bootstrap",
        descriptor: String,
        maxLocals: Int,
    ): MethodInitialFrame =
        build(
            descriptor = descriptor,
            maxLocals = maxLocals,
            thisLocals = listOf(VerificationType.ClassType(currentClass, loader = currentClassLoader)),
            flags = emptyList(),
        )

    private fun build(
        descriptor: String,
        maxLocals: Int,
        thisLocals: List<VerificationType>,
        flags: List<MethodInitialFrameFlag>,
    ): MethodInitialFrame {
        val descriptorTypes = MethodDescriptorVerificationTypeParser.parse(descriptor)
        val initialLocals = thisLocals + VerificationTypeSlotExpander.expand(descriptorTypes.parameterTypes)
        if (initialLocals.size > maxLocals) {
            throw MethodVerificationException(
                "Initial frame locals use ${initialLocals.size} local variable unit(s), exceeding max_locals=$maxLocals",
            )
        }
        val paddedLocals = initialLocals + List(maxLocals - initialLocals.size) { VerificationType.Top }
        return MethodInitialFrame(
            locals = paddedLocals,
            stack = emptyList(),
            flags = flags,
            returnType = descriptorTypes.returnType,
        )
    }
}
