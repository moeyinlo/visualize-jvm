package me.moeyinlo.visualize.jvm.verifier

object FloatReturnInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        declaredReturnType: VerificationReturnType,
        maxStack: Int,
    ) {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val expectedReturnType = VerificationReturnType.Value(VerificationType.Float)
        if (declaredReturnType != expectedReturnType) {
            throw MethodVerificationException("Method return type is $declaredReturnType, expected Float")
        }
        stack.pop(VerificationType.Float)
    }
}
