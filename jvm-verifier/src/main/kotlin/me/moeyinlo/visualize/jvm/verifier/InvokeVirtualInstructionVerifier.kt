package me.moeyinlo.visualize.jvm.verifier

object InvokeVirtualInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        methodOwnerType: VerificationType,
        methodName: String,
        descriptor: String,
        maxStack: Int,
        protectedAccess: ProtectedMemberAccess? = null,
        protectedEnvironment: ProtectedMemberAccessEnvironment? = null,
    ): VerificationFrameState {
        if (methodName == "<init>" || methodName == "<clinit>") {
            throw MethodVerificationException("invokevirtual target method must not be $methodName")
        }
        val methodTypes = MethodDescriptorVerificationTypeParser.parse(descriptor)
        var stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        for (argumentType in methodTypes.parameterTypes.asReversed()) {
            stack = stack.pop(argumentType).stack
        }
        verifyProtectedAccess(
            protectedAccess = protectedAccess,
            protectedEnvironment = protectedEnvironment,
            frame = frame.copy(stack = stack.values),
        )
        stack = stack.pop(methodOwnerType).stack
        if (methodTypes.returnType != null) {
            stack = stack.push(methodTypes.returnType)
        }
        return frame.copy(stack = stack.values)
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
                "invokevirtual protected access verification requires both access and environment",
            )
        }
    }
}
