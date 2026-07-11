package me.moeyinlo.visualize.jvm.verifier

object MonitorInstructionVerifier {
    fun verify(frame: VerificationFrameState, maxStack: Int): VerificationFrameState {
        val withoutObjectRef = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(VerificationType.Reference)
        return frame.copy(stack = withoutObjectRef.stack.values)
    }
}
