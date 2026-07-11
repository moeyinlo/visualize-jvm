package me.moeyinlo.visualize.jvm.verifier

object ReferenceReturnInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        declaredReturnType: VerificationReturnType,
        maxStack: Int,
    ) {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val expectedType = when (declaredReturnType) {
            VerificationReturnType.Void -> throw MethodVerificationException(
                "Method return type is void, expected Reference",
            )
            is VerificationReturnType.Value -> declaredReturnType.type
        }
        if (!expectedType.isAssignableTo(VerificationType.Reference)) {
            throw MethodVerificationException("Method return type is $expectedType, expected Reference")
        }
        stack.pop(expectedType)
    }
}
