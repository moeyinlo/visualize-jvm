package me.moeyinlo.visualize.jvm.verifier

object IntArrayStoreInstructionVerifier {
    private val INT_ARRAY_TYPE = VerificationType.ArrayOf(VerificationType.Integer)

    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue = stack.pop(VerificationType.Integer)
        val withoutIndex = withoutValue.stack.pop(VerificationType.Integer)
        val withoutArrayRef = withoutIndex.stack.pop(INT_ARRAY_TYPE)
        return frame.copy(stack = withoutArrayRef.stack.values)
    }
}
