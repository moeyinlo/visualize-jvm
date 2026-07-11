package me.moeyinlo.visualize.jvm.verifier

object GotoInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        return frame
    }
}
