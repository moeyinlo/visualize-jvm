package me.moeyinlo.visualize.jvm.verifier

object InstanceOfInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val withoutObjectRef = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(VerificationType.Reference)
        val nextStack = withoutObjectRef.stack.push(VerificationType.Integer).values
        return frame.copy(stack = nextStack)
    }
}
