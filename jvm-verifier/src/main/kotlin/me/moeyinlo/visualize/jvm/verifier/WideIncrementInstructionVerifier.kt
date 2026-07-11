package me.moeyinlo.visualize.jvm.verifier

object WideIncrementInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        index: Int,
        maxLocals: Int,
    ): VerificationFrameState =
        IncrementInstructionVerifier.verify(
            frame = frame,
            index = index,
            maxLocals = maxLocals,
        )
}
