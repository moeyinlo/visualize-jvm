package me.moeyinlo.visualize.jvm.verifier

object DoubleArrayStoreInstructionVerifier {
    private val DOUBLE_ARRAY_TYPE = VerificationType.ArrayOf(VerificationType.Double)

    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue = stack.pop(VerificationType.Double)
        val withoutIndex = withoutValue.stack.pop(VerificationType.Integer)
        val withoutArrayRef = withoutIndex.stack.pop(DOUBLE_ARRAY_TYPE)
        return frame.copy(stack = withoutArrayRef.stack.values)
    }
}
