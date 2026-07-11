package me.moeyinlo.visualize.jvm.verifier

object Pop2InstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val top = stack.values.lastOrNull()
            ?: throw MethodVerificationException("Operand stack is empty, expected category 1 or category 2 value")
        val nextStack = if (top.locationCount == 2) {
            stack.popCategory2().stack.values
        } else {
            stack.popCategory1().stack.popCategory1().stack.values
        }
        return frame.copy(stack = nextStack)
    }
}
