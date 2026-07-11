package me.moeyinlo.visualize.jvm.verifier

object GetFieldInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        fieldOwnerType: VerificationType,
        fieldType: VerificationType,
        maxStack: Int,
        protectedAccess: ProtectedMemberAccess? = null,
        protectedEnvironment: ProtectedMemberAccessEnvironment? = null,
    ): VerificationFrameState {
        verifyProtectedAccess(
            protectedAccess = protectedAccess,
            protectedEnvironment = protectedEnvironment,
            frame = frame,
        )
        val withoutReceiver = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(fieldOwnerType)
        val nextStack = withoutReceiver.stack.push(fieldType)
        return frame.copy(stack = nextStack.values)
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
                "getfield protected access verification requires both access and environment",
            )
        }
    }
}
