package me.moeyinlo.visualize.jvm.verifier

object InvokeSpecialInstructionVerifier {
    fun verifyNonInitializer(
        frame: VerificationFrameState,
        thisType: VerificationType,
        methodName: String,
        descriptor: String,
        maxStack: Int,
        methodOwnerType: VerificationType.ClassType? = null,
        ownerEnvironment: InvokeSpecialOwnerEnvironment? = null,
    ): VerificationFrameState {
        if (methodName == "<init>" || methodName == "<clinit>") {
            throw MethodVerificationException("invokespecial non-initializer target method must not be $methodName")
        }
        verifyMethodOwner(methodOwnerType, ownerEnvironment)
        val methodTypes = MethodDescriptorVerificationTypeParser.parse(descriptor)
        var stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        for (argumentType in methodTypes.parameterTypes.asReversed()) {
            stack = stack.pop(argumentType).stack
        }
        stack = stack.pop(thisType).stack
        if (methodTypes.returnType != null) {
            stack = stack.push(methodTypes.returnType)
        }
        return frame.copy(stack = stack.values)
    }

    private fun verifyMethodOwner(
        methodOwnerType: VerificationType.ClassType?,
        ownerEnvironment: InvokeSpecialOwnerEnvironment?,
    ) {
        when {
            methodOwnerType == null && ownerEnvironment == null -> return
            methodOwnerType != null && ownerEnvironment != null -> {
                if (!ownerEnvironment.isValidOwner(methodOwnerType)) {
                    throw MethodVerificationException(
                        "invokespecial non-initializer owner ${methodOwnerType.internalName} is not current class " +
                            "${ownerEnvironment.currentClass.internalName}, a superclass, or a direct superinterface",
                    )
                }
            }
            else -> throw MethodVerificationException(
                "invokespecial non-initializer owner verification requires both owner and environment",
            )
        }
    }
}

data class InvokeSpecialOwnerEnvironment(
    val currentClass: ProtectedVerifierClass,
    val superclasses: List<ProtectedVerifierClass>,
    val directSuperinterfaceNames: List<String>,
) {
    fun isValidOwner(methodOwnerType: VerificationType.ClassType): Boolean =
        methodOwnerType.internalName == currentClass.internalName ||
            superclasses.any { superclass ->
                superclass.internalName == methodOwnerType.internalName &&
                    superclass.definingLoader == methodOwnerType.loader
            } ||
            directSuperinterfaceNames.any { internalName -> internalName == methodOwnerType.internalName }
}
