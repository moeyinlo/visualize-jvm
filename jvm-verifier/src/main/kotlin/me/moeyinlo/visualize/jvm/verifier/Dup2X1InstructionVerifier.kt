package me.moeyinlo.visualize.jvm.verifier

object Dup2X1InstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val top = stack.values.lastOrNull()
            ?: throw MethodVerificationException("Operand stack is empty, expected category 1 or category 2 value")
        val nextStack = if (top.locationCount == 2) {
            val first = stack.popCategory2()
            val second = first.stack.popCategory1()
            second.stack
                .push(first.value)
                .push(second.value)
                .push(first.value)
                .values
        } else {
            val first = stack.popCategory1()
            val second = first.stack.popCategory1()
            val third = second.stack.popCategory1()
            third.stack
                .push(second.value)
                .push(first.value)
                .push(third.value)
                .push(second.value)
                .push(first.value)
                .values
        }
        return frame.copy(stack = nextStack)
    }
}
