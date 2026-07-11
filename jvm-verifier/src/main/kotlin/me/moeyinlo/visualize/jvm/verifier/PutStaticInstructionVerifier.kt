package me.moeyinlo.visualize.jvm.verifier

object PutStaticInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        fieldType: VerificationType,
        maxStack: Int,
    ): VerificationFrameState {
        val withoutValue = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(fieldType)
        return frame.copy(stack = withoutValue.stack.values)
    }
}
