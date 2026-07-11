package me.moeyinlo.visualize.jvm.verifier

object FloatBinaryArithmeticInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        kind: FloatBinaryArithmeticKind,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue2 = stack.pop(kind.operandType)
        val withoutValue1 = withoutValue2.stack.pop(kind.operandType)
        val nextStack = withoutValue1.stack.push(kind.resultType).values
        return frame.copy(stack = nextStack)
    }
}

enum class FloatBinaryArithmeticKind(
    val operandType: VerificationType,
    val resultType: VerificationType,
) {
    Add(VerificationType.Float, VerificationType.Float),
    Divide(VerificationType.Float, VerificationType.Float),
    Multiply(VerificationType.Float, VerificationType.Float),
    Remainder(VerificationType.Float, VerificationType.Float),
    Subtract(VerificationType.Float, VerificationType.Float),
}
