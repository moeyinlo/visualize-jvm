package me.moeyinlo.visualize.jvm.verifier

object InstanceOfInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        targetType: VerificationType,
        maxStack: Int,
    ): VerificationFrameState {
        if (!targetType.isInstanceOfTarget()) {
            throw MethodVerificationException("instanceof target $targetType is not a class or array type")
        }
        val withoutObjectRef = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(VerificationType.Reference)
        val nextStack = withoutObjectRef.stack.push(VerificationType.Integer).values
        return frame.copy(stack = nextStack)
    }

    private fun VerificationType.isInstanceOfTarget(): Boolean =
        this is VerificationType.ObjectType ||
            this is VerificationType.ClassType ||
            this is VerificationType.ArrayOf
}
