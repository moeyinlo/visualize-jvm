package me.moeyinlo.visualize.jvm.verifier

object IntReturnInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        declaredReturnType: VerificationReturnType,
        maxStack: Int,
    ) {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val expectedReturnType = VerificationReturnType.Value(VerificationType.Integer)
        if (declaredReturnType != expectedReturnType) {
            throw MethodVerificationException("Method return type is $declaredReturnType, expected Integer")
        }
        stack.pop(VerificationType.Integer)
    }
}
