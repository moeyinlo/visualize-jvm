package me.moeyinlo.visualize.jvm.verifier

object ANewArrayInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        componentType: VerificationType,
        maxStack: Int,
    ): VerificationFrameState {
        if (!componentType.isAnewarrayComponent()) {
            throw MethodVerificationException(
                "anewarray component $componentType is not a class, interface, or array type",
            )
        }
        val withoutCount = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(VerificationType.Integer)
        val nextStack = withoutCount.stack.push(VerificationType.ArrayOf(componentType)).values
        return frame.copy(stack = nextStack)
    }

    private fun VerificationType.isAnewarrayComponent(): Boolean =
        this is VerificationType.ObjectType ||
            this is VerificationType.ClassType ||
            this is VerificationType.ArrayOf
}
