package me.moeyinlo.visualize.jvm.verifier

object JsrWideInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        maxStack: Int,
    ): VerificationFrameState =
        JsrInstructionVerifier.verify(
            frame = frame,
            maxStack = maxStack,
        )
}
