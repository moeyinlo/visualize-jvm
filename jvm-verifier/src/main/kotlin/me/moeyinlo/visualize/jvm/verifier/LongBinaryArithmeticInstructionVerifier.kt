package me.moeyinlo.visualize.jvm.verifier

object LongBinaryArithmeticInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        kind: LongBinaryArithmeticKind,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue2 = stack.pop(kind.operandType)
        val withoutValue1 = withoutValue2.stack.pop(kind.operandType)
        val nextStack = withoutValue1.stack.push(kind.resultType).values
        return frame.copy(stack = nextStack)
    }
}

enum class LongBinaryArithmeticKind(
    val operandType: VerificationType,
    val resultType: VerificationType,
) {
    Add(VerificationType.Long, VerificationType.Long),
    And(VerificationType.Long, VerificationType.Long),
    Divide(VerificationType.Long, VerificationType.Long),
    Multiply(VerificationType.Long, VerificationType.Long),
    Or(VerificationType.Long, VerificationType.Long),
    Remainder(VerificationType.Long, VerificationType.Long),
    Subtract(VerificationType.Long, VerificationType.Long),
    Xor(VerificationType.Long, VerificationType.Long),
}
