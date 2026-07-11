package me.moeyinlo.visualize.jvm.verifier

object GetStaticInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        fieldType: VerificationType,
        maxStack: Int,
    ): VerificationFrameState {
        val nextStack = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .push(fieldType)
        return frame.copy(stack = nextStack.values)
    }
}
