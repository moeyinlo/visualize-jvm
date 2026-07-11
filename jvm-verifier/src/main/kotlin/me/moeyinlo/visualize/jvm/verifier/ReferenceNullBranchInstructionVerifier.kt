package me.moeyinlo.visualize.jvm.verifier

object ReferenceNullBranchInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val withoutValue = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(VerificationType.Reference)
        return frame.copy(stack = withoutValue.stack.values)
    }
}
