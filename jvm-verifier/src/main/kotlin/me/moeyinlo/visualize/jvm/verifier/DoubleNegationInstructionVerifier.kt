package me.moeyinlo.visualize.jvm.verifier

object DoubleNegationInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue = stack.pop(VerificationType.Double)
        val nextStack = withoutValue.stack.push(VerificationType.Double).values
        return frame.copy(stack = nextStack)
    }
}
