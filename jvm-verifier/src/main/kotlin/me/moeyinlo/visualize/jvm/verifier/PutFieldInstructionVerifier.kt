package me.moeyinlo.visualize.jvm.verifier

object PutFieldInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        fieldOwnerType: VerificationType,
        fieldType: VerificationType,
        maxStack: Int,
        protectedAccess: ProtectedMemberAccess? = null,
        protectedEnvironment: ProtectedMemberAccessEnvironment? = null,
    ): VerificationFrameState {
        val withoutValue = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(fieldType)
        verifyProtectedAccess(
            protectedAccess = protectedAccess,
            protectedEnvironment = protectedEnvironment,
            frame = frame.copy(stack = withoutValue.stack.values),
        )
        val withoutReceiver = withoutValue.stack.pop(fieldOwnerType)
        return frame.copy(stack = withoutReceiver.stack.values)
    }

    private fun verifyProtectedAccess(
        protectedAccess: ProtectedMemberAccess?,
        protectedEnvironment: ProtectedMemberAccessEnvironment?,
        frame: VerificationFrameState,
    ) {
        when {
            protectedAccess == null && protectedEnvironment == null -> return
            protectedAccess != null && protectedEnvironment != null -> ProtectedMemberAccessVerifier.verify(
                access = protectedAccess,
                environment = protectedEnvironment,
                frame = frame,
            )
            else -> throw MethodVerificationException(
                "putfield protected access verification requires both access and environment",
            )
        }
    }
}
