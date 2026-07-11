package me.moeyinlo.visualize.jvm.verifier

object IntArrayLoadInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutIndex = stack.pop(VerificationType.Integer)
        val withoutArrayRef = withoutIndex.stack.pop(VerificationType.ArrayOf(VerificationType.Integer))
        val nextStack = withoutArrayRef.stack.push(VerificationType.Integer).values
        return frame.copy(stack = nextStack)
    }
}
