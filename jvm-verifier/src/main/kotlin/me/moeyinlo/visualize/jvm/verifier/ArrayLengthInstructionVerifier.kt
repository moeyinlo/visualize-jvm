package me.moeyinlo.visualize.jvm.verifier

object ArrayLengthInstructionVerifier {
    private const val EXPECTED_ARRAY_REFERENCE = "array or null reference"

    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val arrayRef = stack.values.lastOrNull()
            ?: throw MethodVerificationException("Operand stack is empty, expected $EXPECTED_ARRAY_REFERENCE")
        if (arrayRef !is VerificationType.ArrayOf && arrayRef != VerificationType.Null) {
            throw MethodVerificationException(
                "Operand stack top contains $arrayRef, expected $EXPECTED_ARRAY_REFERENCE",
            )
        }
        val withoutArrayRef = stack.popCategory1()
        val nextStack = withoutArrayRef.stack.push(VerificationType.Integer).values
        return frame.copy(stack = nextStack)
    }
}
