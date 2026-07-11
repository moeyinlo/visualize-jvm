package me.moeyinlo.visualize.jvm.verifier

object IntBinaryArithmeticInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        kind: IntBinaryArithmeticKind,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue2 = stack.pop(kind.operandType)
        val withoutValue1 = withoutValue2.stack.pop(kind.operandType)
        val nextStack = withoutValue1.stack.push(kind.resultType).values
        return frame.copy(stack = nextStack)
    }
}

enum class IntBinaryArithmeticKind(
    val operandType: VerificationType,
    val resultType: VerificationType,
) {
    Add(VerificationType.Integer, VerificationType.Integer),
    And(VerificationType.Integer, VerificationType.Integer),
    Divide(VerificationType.Integer, VerificationType.Integer),
    Multiply(VerificationType.Integer, VerificationType.Integer),
    Or(VerificationType.Integer, VerificationType.Integer),
    Remainder(VerificationType.Integer, VerificationType.Integer),
    Subtract(VerificationType.Integer, VerificationType.Integer),
    Xor(VerificationType.Integer, VerificationType.Integer),
}
