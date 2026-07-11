package me.moeyinlo.visualize.jvm.verifier

object ReferenceArrayLoadInstructionVerifier {
    private const val EXPECTED_ARRAY_REFERENCE = "array reference with reference component"

    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutIndex = stack.pop(VerificationType.Integer)
        val arrayRef = withoutIndex.stack.values.lastOrNull()
            ?: throw MethodVerificationException("Operand stack is empty, expected $EXPECTED_ARRAY_REFERENCE")
        val withoutArrayRef = withoutIndex.stack.popCategory1()
        val componentType = componentTypeOf(arrayRef)
        val nextStack = withoutArrayRef.stack.push(componentType).values
        return frame.copy(stack = nextStack)
    }

    private fun componentTypeOf(arrayRef: VerificationType): VerificationType =
        when (arrayRef) {
            VerificationType.Null -> VerificationType.Null
            is VerificationType.ArrayOf -> {
                if (!arrayRef.component.isAssignableTo(VerificationType.Reference)) {
                    throw MethodVerificationException(
                        "Operand stack top contains $arrayRef, expected $EXPECTED_ARRAY_REFERENCE",
                    )
                }
                arrayRef.component
            }
            else -> throw MethodVerificationException(
                "Operand stack top contains $arrayRef, expected $EXPECTED_ARRAY_REFERENCE",
            )
        }
}
