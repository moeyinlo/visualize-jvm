package me.moeyinlo.visualize.jvm.verifier

object DupX2InstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val first = stack.popCategory1()
        val secondTop = first.stack.values.lastOrNull()
            ?: throw MethodVerificationException("Operand stack is empty, expected category 1 value")
        val nextStack = if (secondTop.locationCount == 2) {
            val second = first.stack.popCategory2()
            second.stack
                .push(first.value)
                .push(second.value)
                .push(first.value)
                .values
        } else {
            val second = first.stack.popCategory1()
            val third = second.stack.popCategory1()
            third.stack
                .push(first.value)
                .push(third.value)
                .push(second.value)
                .push(first.value)
                .values
        }
        return frame.copy(stack = nextStack)
    }
}
