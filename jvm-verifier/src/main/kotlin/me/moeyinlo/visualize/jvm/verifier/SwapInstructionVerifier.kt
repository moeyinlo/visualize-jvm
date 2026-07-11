package me.moeyinlo.visualize.jvm.verifier

object SwapInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val first = stack.popCategory1()
        val second = first.stack.popCategory1()
        val nextStack = second.stack
            .push(first.value)
            .push(second.value)
            .values
        return frame.copy(stack = nextStack)
    }
}
