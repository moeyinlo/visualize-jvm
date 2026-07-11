package me.moeyinlo.visualize.jvm.verifier

object LongCompareInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue2 = stack.pop(VerificationType.Long)
        val withoutValue1 = withoutValue2.stack.pop(VerificationType.Long)
        val nextStack = withoutValue1.stack.push(VerificationType.Integer).values
        return frame.copy(stack = nextStack)
    }
}
