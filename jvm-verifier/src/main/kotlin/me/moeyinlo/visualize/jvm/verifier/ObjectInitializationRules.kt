package me.moeyinlo.visualize.jvm.verifier

object ObjectInitializationRules {
    fun beginNewObject(
        frame: VerificationFrameState,
        newOffset: Int,
        maxStack: Int,
    ): VerificationFrameState {
        val newItem = VerificationType.Uninitialized(newOffset)
        if (newItem in frame.stack) {
            throw MethodVerificationException(
                "Operand stack already contains uninitialized object created at bytecode offset $newOffset",
            )
        }

        val nextStack = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .push(newItem)
            .values
        return frame.copy(
            locals = frame.locals.replace(newItem, VerificationType.Top),
            stack = nextStack,
        )
    }

    fun completeObjectConstructorInvocation(
        frameAfterPop: VerificationFrameState,
        newOffset: Int,
        objectType: VerificationType.ObjectType,
    ): ConstructorInvocationTransition {
        val newItem = VerificationType.Uninitialized(newOffset)
        val normalFrame = frameAfterPop.copy(
            locals = frameAfterPop.locals.replace(newItem, objectType),
            stack = frameAfterPop.stack.replace(newItem, objectType),
        )
        val exceptionFrame = frameAfterPop.copy(
            locals = frameAfterPop.locals.replace(newItem, VerificationType.Top),
            stack = emptyList(),
        )
        return ConstructorInvocationTransition(
            normalFrame = normalFrame,
            exceptionFrame = exceptionFrame,
        )
    }

    private fun List<VerificationType>.replace(
        old: VerificationType,
        new: VerificationType,
    ): List<VerificationType> =
        map { type ->
            if (type == old) new else type
        }
}
