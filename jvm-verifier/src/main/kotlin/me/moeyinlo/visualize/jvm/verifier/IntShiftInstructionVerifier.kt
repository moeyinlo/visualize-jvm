package me.moeyinlo.visualize.jvm.verifier

object IntShiftInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        kind: IntShiftKind,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutShiftCount = stack.pop(kind.shiftCountType)
        val withoutValue = withoutShiftCount.stack.pop(kind.valueType)
        val nextStack = withoutValue.stack.push(kind.resultType).values
        return frame.copy(stack = nextStack)
    }
}

enum class IntShiftKind(
    val valueType: VerificationType,
    val shiftCountType: VerificationType,
    val resultType: VerificationType,
) {
    Left(VerificationType.Integer, VerificationType.Integer, VerificationType.Integer),
    Right(VerificationType.Integer, VerificationType.Integer, VerificationType.Integer),
    UnsignedRight(VerificationType.Integer, VerificationType.Integer, VerificationType.Integer),
}
