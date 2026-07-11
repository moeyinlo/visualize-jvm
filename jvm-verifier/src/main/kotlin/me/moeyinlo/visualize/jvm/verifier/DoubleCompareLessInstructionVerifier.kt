package me.moeyinlo.visualize.jvm.verifier

object DoubleCompareLessInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue2 = stack.pop(VerificationType.Double)
        val withoutValue1 = withoutValue2.stack.pop(VerificationType.Double)
        val nextStack = withoutValue1.stack.push(VerificationType.Integer).values
        return frame.copy(stack = nextStack)
    }
}
