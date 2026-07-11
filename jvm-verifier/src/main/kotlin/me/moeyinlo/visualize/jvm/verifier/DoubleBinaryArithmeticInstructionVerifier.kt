package me.moeyinlo.visualize.jvm.verifier

object DoubleBinaryArithmeticInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        kind: DoubleBinaryArithmeticKind,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue2 = stack.pop(kind.operandType)
        val withoutValue1 = withoutValue2.stack.pop(kind.operandType)
        val nextStack = withoutValue1.stack.push(kind.resultType).values
        return frame.copy(stack = nextStack)
    }
}

enum class DoubleBinaryArithmeticKind(
    val operandType: VerificationType,
    val resultType: VerificationType,
) {
    Add(VerificationType.Double, VerificationType.Double),
    Divide(VerificationType.Double, VerificationType.Double),
    Multiply(VerificationType.Double, VerificationType.Double),
    Remainder(VerificationType.Double, VerificationType.Double),
    Subtract(VerificationType.Double, VerificationType.Double),
}
