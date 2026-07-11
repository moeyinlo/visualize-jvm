package me.moeyinlo.visualize.jvm.verifier

object GetFieldInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        fieldOwnerType: VerificationType,
        fieldType: VerificationType,
        maxStack: Int,
    ): VerificationFrameState {
        val withoutReceiver = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(fieldOwnerType)
        val nextStack = withoutReceiver.stack.push(fieldType)
        return frame.copy(stack = nextStack.values)
    }
}
