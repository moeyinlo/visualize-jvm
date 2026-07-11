package me.moeyinlo.visualize.jvm.verifier

object DupInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val top = stack.popCategory1().value
        return frame.copy(stack = stack.push(top).values)
    }
}
