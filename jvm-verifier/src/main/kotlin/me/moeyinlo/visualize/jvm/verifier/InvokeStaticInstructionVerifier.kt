package me.moeyinlo.visualize.jvm.verifier

object InvokeStaticInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        descriptor: String,
        maxStack: Int,
    ): VerificationFrameState {
        val methodTypes = MethodDescriptorVerificationTypeParser.parse(descriptor)
        var stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        for (argumentType in methodTypes.parameterTypes.asReversed()) {
            stack = stack.pop(argumentType).stack
        }
        if (methodTypes.returnType != null) {
            stack = stack.push(methodTypes.returnType)
        }
        return frame.copy(stack = stack.values)
    }
}
