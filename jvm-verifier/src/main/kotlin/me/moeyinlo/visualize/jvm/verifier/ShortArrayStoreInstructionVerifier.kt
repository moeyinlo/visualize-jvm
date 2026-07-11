package me.moeyinlo.visualize.jvm.verifier

object ShortArrayStoreInstructionVerifier {
    private val SHORT_ARRAY_TYPE = VerificationType.ArrayOf(VerificationType.Short)

    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue = stack.pop(VerificationType.Integer)
        val withoutIndex = withoutValue.stack.pop(VerificationType.Integer)
        val withoutArrayRef = withoutIndex.stack.pop(SHORT_ARRAY_TYPE)
        return frame.copy(stack = withoutArrayRef.stack.values)
    }
}
