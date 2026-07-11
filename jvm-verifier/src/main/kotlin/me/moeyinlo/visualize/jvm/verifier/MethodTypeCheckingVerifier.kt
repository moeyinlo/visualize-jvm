package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.CodeAttribute

object MethodTypeCheckingVerifier {
    fun verify(
        code: CodeAttribute,
        frameStates: Iterable<VerificationFrameState>,
    ) {
        MethodResourceLimitsVerifier.verify(code = code, frameStates = frameStates)
        MethodControlFlowGraphBuilder.build(code)
    }
}
