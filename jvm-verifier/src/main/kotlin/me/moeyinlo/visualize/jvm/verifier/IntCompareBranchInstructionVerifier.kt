package me.moeyinlo.visualize.jvm.verifier

object IntCompareBranchInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue2 = stack.pop(VerificationType.Integer)
        val withoutValue1 = withoutValue2.stack.pop(VerificationType.Integer)
        return frame.copy(stack = withoutValue1.stack.values)
    }
}
