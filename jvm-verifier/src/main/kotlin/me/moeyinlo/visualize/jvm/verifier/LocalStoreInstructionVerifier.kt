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
            .popStoreValue(kind)
        val localVariables = VerifierLocalVariables
            .fromCompact(locals = frame.locals, maxLocals = maxLocals)
            .store(index = index, value = popped.value)
        return frame.copy(
            locals = localVariables.slots,
            stack = popped.stack.values,
        )
    }
}

private fun VerifierOperandStack.popStoreValue(kind: LocalStoreKind): VerifierOperandStackPop =
    when (kind) {
        LocalStoreKind.Reference -> popReferenceOrReturnAddress()
        else -> pop(kind.expectedType)
    }

private fun VerifierOperandStack.popReferenceOrReturnAddress(): VerifierOperandStackPop {
    val popped = popCategory1()
    val value = popped.value
    if (value != VerificationType.ReturnAddress && !value.isAssignableTo(VerificationType.Reference)) {
        throw MethodVerificationException(
            "Operand stack top contains $value, expected Reference or ReturnAddress",
        )
    }
    return popped
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
