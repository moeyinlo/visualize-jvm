package me.moeyinlo.visualize.jvm.verifier

object IncrementInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        index: Int,
        maxLocals: Int,
    ): VerificationFrameState {
        VerifierLocalVariables
            .fromCompact(locals = frame.locals, maxLocals = maxLocals)
            .load(index = index, expected = VerificationType.Integer)
        return frame
    }
}
