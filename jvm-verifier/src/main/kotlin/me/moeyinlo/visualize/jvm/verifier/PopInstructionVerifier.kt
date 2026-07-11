package me.moeyinlo.visualize.jvm.verifier

object PopInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val nextStack = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .popCategory1()
            .stack
            .values
        return frame.copy(stack = nextStack)
    }
}
