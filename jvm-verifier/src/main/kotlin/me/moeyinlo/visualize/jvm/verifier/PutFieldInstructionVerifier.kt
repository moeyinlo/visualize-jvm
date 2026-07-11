package me.moeyinlo.visualize.jvm.verifier

object PutFieldInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        fieldOwnerType: VerificationType,
        fieldType: VerificationType,
        maxStack: Int,
    ): VerificationFrameState {
        val withoutValue = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(fieldType)
        val withoutReceiver = withoutValue.stack.pop(fieldOwnerType)
        return frame.copy(stack = withoutReceiver.stack.values)
    }
}
