package me.moeyinlo.visualize.jvm.verifier

object InvokeDynamicInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        callSiteName: String,
        descriptor: String,
        maxStack: Int,
    ): VerificationFrameState {
        if (callSiteName == "<init>" || callSiteName == "<clinit>") {
            throw MethodVerificationException("invokedynamic call site name must not be $callSiteName")
        }
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
