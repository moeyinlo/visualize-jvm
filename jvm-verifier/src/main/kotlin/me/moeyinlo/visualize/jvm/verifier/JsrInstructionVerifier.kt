package me.moeyinlo.visualize.jvm.verifier

object JsrInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState {
        val stack = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .push(VerificationType.ReturnAddress)
        return frame.copy(stack = stack.values)
    }
}
