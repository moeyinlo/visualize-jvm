package me.moeyinlo.visualize.jvm.verifier

object InvokeSpecialInstructionVerifier {
    fun verifyNonInitializer(
        frame: VerificationFrameState,
        thisType: VerificationType,
        methodName: String,
        descriptor: String,
        maxStack: Int,
    ): VerificationFrameState {
        if (methodName == "<init>" || methodName == "<clinit>") {
            throw MethodVerificationException("invokespecial non-initializer target method must not be $methodName")
        }
        val methodTypes = MethodDescriptorVerificationTypeParser.parse(descriptor)
        var stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        for (argumentType in methodTypes.parameterTypes.asReversed()) {
            stack = stack.pop(argumentType).stack
        }
        stack = stack.pop(thisType).stack
        if (methodTypes.returnType != null) {
            stack = stack.push(methodTypes.returnType)
        }
        return frame.copy(stack = stack.values)
    }
}
