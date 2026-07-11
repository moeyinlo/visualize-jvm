package me.moeyinlo.visualize.jvm.verifier

object SwitchInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val withoutKey = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(VerificationType.Integer)
        return frame.copy(stack = withoutKey.stack.values)
    }
}
