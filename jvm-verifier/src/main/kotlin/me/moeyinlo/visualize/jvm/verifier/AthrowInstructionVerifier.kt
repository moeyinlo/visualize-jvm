package me.moeyinlo.visualize.jvm.verifier

object AthrowInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        throwableType: VerificationType.ObjectType,
        maxStack: Int,
    ) {
        VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(throwableType)
    }
}
