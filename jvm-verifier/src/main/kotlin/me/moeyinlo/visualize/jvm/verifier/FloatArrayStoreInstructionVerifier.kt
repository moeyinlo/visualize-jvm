package me.moeyinlo.visualize.jvm.verifier

object FloatArrayStoreInstructionVerifier {
    private val FLOAT_ARRAY_TYPE = VerificationType.ArrayOf(VerificationType.Float)

    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue = stack.pop(VerificationType.Float)
        val withoutIndex = withoutValue.stack.pop(VerificationType.Integer)
        val withoutArrayRef = withoutIndex.stack.pop(FLOAT_ARRAY_TYPE)
        return frame.copy(stack = withoutArrayRef.stack.values)
    }
}
