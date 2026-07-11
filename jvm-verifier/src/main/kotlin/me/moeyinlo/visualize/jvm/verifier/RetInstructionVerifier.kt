package me.moeyinlo.visualize.jvm.verifier

object RetInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        index: Int,
        maxLocals: Int,
    ): VerificationFrameState {
        VerifierLocalVariables
            .fromCompact(locals = frame.locals, maxLocals = maxLocals)
            .load(index = index, expected = VerificationType.ReturnAddress)
        return frame
    }
}
