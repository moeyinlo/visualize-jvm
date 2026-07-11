package me.moeyinlo.visualize.jvm.verifier

object LongShiftInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        kind: LongShiftKind,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutShiftCount = stack.pop(kind.shiftCountType)
        val withoutValue = withoutShiftCount.stack.pop(kind.valueType)
        val nextStack = withoutValue.stack.push(kind.resultType).values
        return frame.copy(stack = nextStack)
    }
}

enum class LongShiftKind(
    val valueType: VerificationType,
    val shiftCountType: VerificationType,
    val resultType: VerificationType,
) {
    Left(VerificationType.Long, VerificationType.Integer, VerificationType.Long),
    Right(VerificationType.Long, VerificationType.Integer, VerificationType.Long),
    UnsignedRight(VerificationType.Long, VerificationType.Integer, VerificationType.Long),
}
