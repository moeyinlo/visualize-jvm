package me.moeyinlo.visualize.jvm.verifier

object CheckCastInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        targetType: VerificationType,
        maxStack: Int,
    ): VerificationFrameState {
        if (!targetType.isCheckCastTarget()) {
            throw MethodVerificationException("checkcast target $targetType is not a class or array type")
        }
        val withoutObjectRef = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(VerificationType.Reference)
        val nextStack = withoutObjectRef.stack.push(targetType).values
        return frame.copy(stack = nextStack)
    }

    private fun VerificationType.isCheckCastTarget(): Boolean =
        this is VerificationType.ObjectType ||
            this is VerificationType.ClassType ||
            this is VerificationType.ArrayOf
}
