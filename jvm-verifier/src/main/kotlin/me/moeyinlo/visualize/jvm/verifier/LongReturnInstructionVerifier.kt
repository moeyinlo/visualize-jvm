package me.moeyinlo.visualize.jvm.verifier

object LongReturnInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        declaredReturnType: VerificationReturnType,
        maxStack: Int,
    ) {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val expectedReturnType = VerificationReturnType.Value(VerificationType.Long)
        if (declaredReturnType != expectedReturnType) {
            throw MethodVerificationException("Method return type is $declaredReturnType, expected Long")
        }
        stack.pop(VerificationType.Long)
    }
}
