package me.moeyinlo.visualize.jvm.verifier

object ReferenceArrayStoreInstructionVerifier {
    private const val EXPECTED_ARRAY_REFERENCE = "array reference with reference component"

    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue = stack.pop(VerificationType.Reference)
        val withoutIndex = withoutValue.stack.pop(VerificationType.Integer)
        val arrayRef = withoutIndex.stack.values.lastOrNull()
            ?: throw MethodVerificationException("Operand stack is empty, expected $EXPECTED_ARRAY_REFERENCE")
        val withoutArrayRef = withoutIndex.stack.popCategory1()
        if (!arrayRef.isReferenceArrayReference()) {
            throw MethodVerificationException(
                "Operand stack top contains $arrayRef, expected $EXPECTED_ARRAY_REFERENCE",
            )
        }
        return frame.copy(stack = withoutArrayRef.stack.values)
    }

    private fun VerificationType.isReferenceArrayReference(): kotlin.Boolean =
        this == VerificationType.Null ||
            this is VerificationType.ArrayOf && component.isAssignableTo(VerificationType.Reference)
}
