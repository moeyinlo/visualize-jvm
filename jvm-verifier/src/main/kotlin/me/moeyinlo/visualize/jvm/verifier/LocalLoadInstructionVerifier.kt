package me.moeyinlo.visualize.jvm.verifier

object LocalLoadInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        index: Int,
        kind: LocalLoadKind,
        maxLocals: Int,
        maxStack: Int,
    ): VerificationFrameState {
        val localVariables = VerifierLocalVariables.fromCompact(
            locals = frame.locals,
            maxLocals = maxLocals,
        )
        val actualType = localVariables.load(index = index, expected = kind.expectedType)
        val nextStack = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .push(actualType)
            .values
        return frame.copy(stack = nextStack)
    }
}

enum class LocalLoadKind(
    val expectedType: VerificationType,
) {
    Int(VerificationType.Integer),
    Long(VerificationType.Long),
    Float(VerificationType.Float),
    Double(VerificationType.Double),
    Reference(VerificationType.Reference),
}
