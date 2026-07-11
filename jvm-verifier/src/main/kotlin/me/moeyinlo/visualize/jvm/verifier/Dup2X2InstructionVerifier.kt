package me.moeyinlo.visualize.jvm.verifier

object Dup2X2InstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val top = stack.values.lastOrNull()
            ?: throw MethodVerificationException("Operand stack is empty, expected category 1 or category 2 value")
        val nextStack = if (top.locationCount == 2) {
            duplicateCategory2Top(stack)
        } else {
            duplicateCategory1PairTop(stack)
        }
        return frame.copy(stack = nextStack)
    }

    private fun duplicateCategory2Top(stack: VerifierOperandStack): List<VerificationType> {
        val first = stack.popCategory2()
        val secondTop = first.stack.values.lastOrNull()
            ?: throw MethodVerificationException("Operand stack is empty, expected category 1 or category 2 value")
        return if (secondTop.locationCount == 2) {
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
    }

    private fun duplicateCategory1PairTop(stack: VerifierOperandStack): List<VerificationType> {
        val first = stack.popCategory1()
        val second = first.stack.popCategory1()
        val thirdTop = second.stack.values.lastOrNull()
            ?: throw MethodVerificationException("Operand stack is empty, expected category 1 or category 2 value")
        return if (thirdTop.locationCount == 2) {
            val third = second.stack.popCategory2()
            third.stack
                .push(second.value)
                .push(first.value)
                .push(third.value)
                .push(second.value)
                .push(first.value)
                .values
        } else {
            val third = second.stack.popCategory1()
            val fourth = third.stack.popCategory1()
            fourth.stack
                .push(second.value)
                .push(first.value)
                .push(fourth.value)
                .push(third.value)
                .push(second.value)
                .push(first.value)
                .values
        }
    }
}
