package me.moeyinlo.visualize.jvm.verifier

object InvokeInterfaceInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        methodOwnerType: VerificationType,
        methodName: String,
        descriptor: String,
        count: Int,
        maxStack: Int,
    ): VerificationFrameState {
        if (methodName == "<init>" || methodName == "<clinit>") {
            throw MethodVerificationException("invokeinterface target method must not be $methodName")
        }
        val methodTypes = MethodDescriptorVerificationTypeParser.parse(descriptor)
        val expectedOperandCount = 1 + methodTypes.parameterTypes.sumOf { type -> type.locationCount }
        var stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        for (argumentType in methodTypes.parameterTypes.asReversed()) {
            stack = stack.pop(argumentType).stack
        }
        stack = stack.pop(methodOwnerType).stack
        if (count != expectedOperandCount) {
            throw MethodVerificationException(
                "invokeinterface count operand $count does not match popped operand count $expectedOperandCount",
            )
        }
        if (methodTypes.returnType != null) {
            stack = stack.push(methodTypes.returnType)
        }
        return frame.copy(stack = stack.values)
    }
}
