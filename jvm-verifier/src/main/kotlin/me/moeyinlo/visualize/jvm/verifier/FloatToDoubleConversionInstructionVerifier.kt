package me.moeyinlo.visualize.jvm.verifier

object FloatToDoubleConversionInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        val withoutValue = stack.pop(VerificationType.Float)
        val nextStack = withoutValue.stack.push(VerificationType.Double).values
        return frame.copy(stack = nextStack)
    }
}
