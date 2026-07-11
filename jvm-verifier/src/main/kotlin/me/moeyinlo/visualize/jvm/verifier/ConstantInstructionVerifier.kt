package me.moeyinlo.visualize.jvm.verifier

object ConstantInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        kind: ConstantPushKind,
        maxStack: Int,
    ): VerificationFrameState {
        val nextStack = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .push(kind.pushedType)
            .values
        return frame.copy(stack = nextStack)
    }
}

enum class ConstantPushKind(
    val pushedType: VerificationType,
) {
    Null(VerificationType.Null),
    Int(VerificationType.Integer),
    Long(VerificationType.Long),
    Float(VerificationType.Float),
    Double(VerificationType.Double),
}
