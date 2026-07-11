package me.moeyinlo.visualize.jvm.verifier

object InvokeVirtualInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        methodOwnerType: VerificationType,
        methodName: String,
        descriptor: String,
        maxStack: Int,
    ): VerificationFrameState {
        if (methodName == "<init>" || methodName == "<clinit>") {
            throw MethodVerificationException("invokevirtual target method must not be $methodName")
        }
        val methodTypes = MethodDescriptorVerificationTypeParser.parse(descriptor)
        var stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        for (argumentType in methodTypes.parameterTypes.asReversed()) {
            stack = stack.pop(argumentType).stack
        }
        stack = stack.pop(methodOwnerType).stack
        if (methodTypes.returnType != null) {
            stack = stack.push(methodTypes.returnType)
        }
        return frame.copy(stack = stack.values)
    }
}
