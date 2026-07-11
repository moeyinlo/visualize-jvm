package me.moeyinlo.visualize.jvm.verifier

object WideLocalStoreInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        index: Int,
        kind: LocalStoreKind,
        maxLocals: Int,
        maxStack: Int,
    ): VerificationFrameState =
        LocalStoreInstructionVerifier.verify(
            frame = frame,
            index = index,
            kind = kind,
            maxLocals = maxLocals,
            maxStack = maxStack,
        )
}
