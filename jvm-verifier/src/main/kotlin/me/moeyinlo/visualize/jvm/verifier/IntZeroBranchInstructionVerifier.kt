package me.moeyinlo.visualize.jvm.verifier

object IntZeroBranchInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val withoutValue = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(VerificationType.Integer)
        return frame.copy(stack = withoutValue.stack.values)
    }
}
