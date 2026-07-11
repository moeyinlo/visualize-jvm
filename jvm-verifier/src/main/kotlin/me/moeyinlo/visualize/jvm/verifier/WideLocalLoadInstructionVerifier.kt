package me.moeyinlo.visualize.jvm.verifier

object WideLocalLoadInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        index: Int,
        kind: LocalLoadKind,
        maxLocals: Int,
        maxStack: Int,
    ): VerificationFrameState =
        LocalLoadInstructionVerifier.verify(
            frame = frame,
            index = index,
            kind = kind,
            maxLocals = maxLocals,
            maxStack = maxStack,
        )
}
