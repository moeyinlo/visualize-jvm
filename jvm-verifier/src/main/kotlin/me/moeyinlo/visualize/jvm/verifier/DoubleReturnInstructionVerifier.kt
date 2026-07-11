package me.moeyinlo.visualize.jvm.verifier

object DoubleReturnInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        declaredReturnType: VerificationReturnType,
        maxStack: Int,
    ) {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val expectedReturnType = VerificationReturnType.Value(VerificationType.Double)
        if (declaredReturnType != expectedReturnType) {
            throw MethodVerificationException("Method return type is $declaredReturnType, expected Double")
        }
        stack.pop(VerificationType.Double)
    }
}
