package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.CodeExceptionHandler

object ExceptionHandlerEdgesVerifier {
    fun handlerFrame(
        handler: CodeExceptionHandler,
        exceptionStackFrame: VerificationFrameState,
        throwableType: VerificationType.ObjectType,
        maxStack: Int,
    ): VerificationFrameState {
        if (maxStack < 1) {
            throw MethodVerificationException(
                "Exception handler at bytecode offset ${handler.handlerPc} requires operand stack depth 1, " +
                    "exceeding max_stack=$maxStack",
            )
        }
        val exceptionType = handler.catchType
            ?.let { catchType -> VerificationType.ObjectType(catchType) }
            ?: throwableType
        return VerificationFrameState(
            bytecodeOffset = handler.handlerPc,
            locals = exceptionStackFrame.locals,
            stack = listOf(exceptionType),
        )
    }

    fun verifyTargetFrame(
        handlerFrame: VerificationFrameState,
        targetFrame: VerificationFrameState,
    ) {
        requireSameSize(
            role = "locals",
            actual = handlerFrame.locals,
            expected = targetFrame.locals,
            targetOffset = targetFrame.bytecodeOffset,
        )
        requireSameSize(
            role = "stack",
            actual = handlerFrame.stack,
            expected = targetFrame.stack,
            targetOffset = targetFrame.bytecodeOffset,
        )
        handlerFrame.locals.requireAssignableTo(
            role = "locals",
            expected = targetFrame.locals,
            targetOffset = targetFrame.bytecodeOffset,
        )
        handlerFrame.stack.requireAssignableTo(
            role = "stack",
            expected = targetFrame.stack,
            targetOffset = targetFrame.bytecodeOffset,
        )
    }

    private fun requireSameSize(
        role: String,
        actual: List<VerificationType>,
        expected: List<VerificationType>,
        targetOffset: Int,
    ) {
        if (actual.size != expected.size) {
            throw MethodVerificationException(
                "Exception handler target $targetOffset $role size ${actual.size} does not match " +
                    "stack map size ${expected.size}",
            )
        }
    }

    private fun List<VerificationType>.requireAssignableTo(
        role: String,
        expected: List<VerificationType>,
        targetOffset: Int,
    ) {
        zip(expected).forEachIndexed { index, (actualType, expectedType) ->
            if (!actualType.isAssignableTo(expectedType)) {
                throw MethodVerificationException(
                    "Exception handler target $targetOffset $role[$index] contains $actualType, " +
                        "expected $expectedType",
                )
            }
        }
    }
}
