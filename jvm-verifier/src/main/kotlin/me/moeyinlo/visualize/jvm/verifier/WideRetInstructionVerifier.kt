package me.moeyinlo.visualize.jvm.verifier

object WideRetInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        index: Int,
        maxLocals: Int,
    ): VerificationFrameState =
        RetInstructionVerifier.verify(
            frame = frame,
            index = index,
            maxLocals = maxLocals,
        )
}
