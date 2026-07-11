package me.moeyinlo.visualize.jvm.verifier

object UninitializedThisRules {
    fun initialThisType(
        isInstanceConstructor: Boolean,
        hasSuperclass: Boolean,
        thisType: VerificationType.ObjectType,
    ): VerificationType =
        if (isInstanceConstructor && hasSuperclass) {
            VerificationType.UninitializedThis
        } else {
            thisType
        }

    fun completeConstructorInvocation(
        frameAfterPop: VerificationFrameState,
        thisType: VerificationType.ObjectType,
    ): ConstructorInvocationTransition {
        val normalFrame = frameAfterPop.copy(
            locals = frameAfterPop.locals.replaceUninitializedThisWith(thisType),
            stack = frameAfterPop.stack.replaceUninitializedThisWith(thisType),
        )
        val exceptionFrame = frameAfterPop.copy(
            locals = frameAfterPop.locals.replaceUninitializedThisWith(VerificationType.Top),
            stack = emptyList(),
        )
        return ConstructorInvocationTransition(
            normalFrame = normalFrame,
            exceptionFrame = exceptionFrame,
        )
    }

    fun requireInitializedThisForReturn(frame: VerificationFrameState) {
        if (VerificationType.UninitializedThis in frame.locals) {
            throw MethodVerificationException(
                "Cannot return from constructor while uninitializedThis is still present at bytecode offset " +
                    frame.bytecodeOffset,
            )
        }
    }

    private fun List<VerificationType>.replaceUninitializedThisWith(
        replacement: VerificationType,
    ): List<VerificationType> =
        map { type ->
            if (type == VerificationType.UninitializedThis) replacement else type
        }
}

data class ConstructorInvocationTransition(
    val normalFrame: VerificationFrameState,
    val exceptionFrame: VerificationFrameState,
)
