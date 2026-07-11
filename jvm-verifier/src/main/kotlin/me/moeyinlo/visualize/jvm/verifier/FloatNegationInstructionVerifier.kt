package me.moeyinlo.visualize.jvm.verifier

object FloatNegationInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue = stack.pop(VerificationType.Float)
        val nextStack = withoutValue.stack.push(VerificationType.Float).values
        return frame.copy(stack = nextStack)
    }
}
