package me.moeyinlo.visualize.jvm.verifier

object ByteArrayLoadInstructionVerifier {
    private const val EXPECTED_ARRAY_REFERENCE = "byte boolean or null array reference"

    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutIndex = stack.pop(VerificationType.Integer)
        val arrayRef = withoutIndex.stack.values.lastOrNull()
            ?: throw MethodVerificationException("Operand stack is empty, expected $EXPECTED_ARRAY_REFERENCE")
        val withoutArrayRef = withoutIndex.stack.popCategory1()
        if (!arrayRef.isSmallArrayReference()) {
            throw MethodVerificationException(
                "Operand stack top contains $arrayRef, expected $EXPECTED_ARRAY_REFERENCE",
            )
        }
        val nextStack = withoutArrayRef.stack.push(VerificationType.Integer).values
        return frame.copy(stack = nextStack)
    }

    private fun VerificationType.isSmallArrayReference(): kotlin.Boolean =
        when (this) {
            VerificationType.Null -> true
            is VerificationType.ArrayOf ->
                component == VerificationType.Byte || component == VerificationType.Boolean
            else -> false
        }
}
