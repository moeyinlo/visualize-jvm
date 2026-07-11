package me.moeyinlo.visualize.jvm.verifier

object LocalStoreInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        index: Int,
        kind: LocalStoreKind,
        maxLocals: Int,
        maxStack: Int,
    ): VerificationFrameState {
        val popped = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(kind.expectedType)
        val localVariables = VerifierLocalVariables
            .fromCompact(locals = frame.locals, maxLocals = maxLocals)
            .store(index = index, value = popped.value)
        return frame.copy(
            locals = localVariables.slots,
            stack = popped.stack.values,
        )
    }
}

enum class LocalStoreKind(
    val expectedType: VerificationType,
) {
    Int(VerificationType.Integer),
    Long(VerificationType.Long),
    Float(VerificationType.Float),
    Double(VerificationType.Double),
    Reference(VerificationType.Reference),
}
