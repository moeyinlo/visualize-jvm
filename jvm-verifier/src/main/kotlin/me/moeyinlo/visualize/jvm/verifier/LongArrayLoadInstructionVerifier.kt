package me.moeyinlo.visualize.jvm.verifier

object LongArrayLoadInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutIndex = stack.pop(VerificationType.Integer)
        val withoutArrayRef = withoutIndex.stack.pop(VerificationType.ArrayOf(VerificationType.Long))
        val nextStack = withoutArrayRef.stack.push(VerificationType.Long).values
        return frame.copy(stack = nextStack)
    }
}
