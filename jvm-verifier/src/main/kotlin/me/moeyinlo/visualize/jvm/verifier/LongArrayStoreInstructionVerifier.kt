package me.moeyinlo.visualize.jvm.verifier

object LongArrayStoreInstructionVerifier {
    private val LONG_ARRAY_TYPE = VerificationType.ArrayOf(VerificationType.Long)

    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue = stack.pop(VerificationType.Long)
        val withoutIndex = withoutValue.stack.pop(VerificationType.Integer)
        val withoutArrayRef = withoutIndex.stack.pop(LONG_ARRAY_TYPE)
        return frame.copy(stack = withoutArrayRef.stack.values)
    }
}
