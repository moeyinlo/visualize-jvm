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

    fun verifyUninitializedThisInitializer(
        frame: VerificationFrameState,
        descriptor: String,
        maxStack: Int,
        methodOwnerType: VerificationType.ClassType,
        ownerEnvironment: InvokeSpecialOwnerEnvironment,
        initializedThisType: VerificationType.ObjectType,
    ): ConstructorInvocationTransition {
        verifyThisInitializerOwner(methodOwnerType, ownerEnvironment)
        val methodTypes = parseVoidInitializerDescriptor(descriptor)
        var stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        for (argumentType in methodTypes.parameterTypes.asReversed()) {
            stack = stack.pop(argumentType).stack
        }
        stack = stack.pop(VerificationType.UninitializedThis).stack
        return UninitializedThisRules.completeConstructorInvocation(
            frameAfterPop = frame.copy(stack = stack.values),
            thisType = initializedThisType,
        )
    }

    fun verifyUninitializedObjectInitializer(
        frame: VerificationFrameState,
        descriptor: String,
        maxStack: Int,
        newOffset: Int,
        methodOwnerType: VerificationType.ObjectType,
        newInstructionObjectType: VerificationType.ObjectType,
    ): ConstructorInvocationTransition {
        verifyNewInstructionOwner(
            newOffset = newOffset,
            methodOwnerType = methodOwnerType,
            newInstructionObjectType = newInstructionObjectType,
        )
        val methodTypes = parseVoidInitializerDescriptor(descriptor)
        var stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        for (argumentType in methodTypes.parameterTypes.asReversed()) {
            stack = stack.pop(argumentType).stack
        }
        stack = stack.pop(VerificationType.Uninitialized(newOffset)).stack
        return ObjectInitializationRules.completeObjectConstructorInvocation(
            frameAfterPop = frame.copy(stack = stack.values),
            newOffset = newOffset,
            objectType = methodOwnerType,
        )
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

    private fun verifyThisInitializerOwner(
        methodOwnerType: VerificationType.ClassType,
        ownerEnvironment: InvokeSpecialOwnerEnvironment,
    ) {
        if (!ownerEnvironment.isValidThisInitializerOwner(methodOwnerType)) {
            throw MethodVerificationException(
                "invokespecial initializer owner ${methodOwnerType.internalName} is not current class " +
                    "${ownerEnvironment.currentClass.internalName} or its direct superclass",
            )
        }
    }

    private fun verifyNewInstructionOwner(
        newOffset: Int,
        methodOwnerType: VerificationType.ObjectType,
        newInstructionObjectType: VerificationType.ObjectType,
    ) {
        if (methodOwnerType != newInstructionObjectType) {
            throw MethodVerificationException(
                "invokespecial initializer owner $methodOwnerType does not match " +
                    "new instruction type $newInstructionObjectType at bytecode offset $newOffset",
            )
        }
    }

    private fun parseVoidInitializerDescriptor(descriptor: String): MethodDescriptorVerificationTypes {
        val methodTypes = MethodDescriptorVerificationTypeParser.parse(descriptor)
        if (methodTypes.returnType != null) {
            throw MethodVerificationException("invokespecial initializer descriptor must return void")
        }
        return methodTypes
    }
}

data class InvokeSpecialOwnerEnvironment(
    val currentClass: ProtectedVerifierClass,
    val superclasses: List<ProtectedVerifierClass>,
    val directSuperinterfaceNames: List<String>,
    val directSuperclassName: String? = superclasses.firstOrNull()?.internalName,
) {
    fun isValidOwner(methodOwnerType: VerificationType.ClassType): Boolean =
        methodOwnerType.internalName == currentClass.internalName ||
            superclasses.any { superclass ->
                superclass.internalName == methodOwnerType.internalName &&
                    superclass.definingLoader == methodOwnerType.loader
            } ||
            directSuperinterfaceNames.any { internalName -> internalName == methodOwnerType.internalName }

    fun isValidThisInitializerOwner(methodOwnerType: VerificationType.ClassType): Boolean =
        methodOwnerType.internalName == currentClass.internalName ||
            methodOwnerType.internalName == directSuperclassName
}
