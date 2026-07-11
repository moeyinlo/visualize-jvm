package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.CodeExceptionHandler
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class ExceptionHandlerEdgesVerifierTest {
    @Test
    fun `derives handler frame with the caught exception on the operand stack`() {
        val catchType = VerificationType.ObjectType(ConstantPoolIndex(7))
        val throwableType = VerificationType.ObjectType(ConstantPoolIndex(9))
        val exceptionStackFrame = VerificationFrameState(
            bytecodeOffset = 4,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Null),
        )

        val handlerFrame = ExceptionHandlerEdgesVerifier.handlerFrame(
            handler = CodeExceptionHandler(
                startPc = 0,
                endPc = 5,
                handlerPc = 8,
                catchType = ConstantPoolIndex(7),
            ),
            exceptionStackFrame = exceptionStackFrame,
            throwableType = throwableType,
            maxStack = 1,
        )

        assertEquals(
            VerificationFrameState(
                bytecodeOffset = 8,
                locals = listOf(VerificationType.Integer),
                stack = listOf(catchType),
            ),
            handlerFrame,
        )
    }

    @Test
    fun `catch all handler uses Throwable as the exception type`() {
        val throwableType = VerificationType.ObjectType(ConstantPoolIndex(9))

        val handlerFrame = ExceptionHandlerEdgesVerifier.handlerFrame(
            handler = CodeExceptionHandler(
                startPc = 0,
                endPc = 5,
                handlerPc = 8,
                catchType = null,
            ),
            exceptionStackFrame = VerificationFrameState(
                bytecodeOffset = 4,
                locals = emptyList(),
                stack = emptyList(),
            ),
            throwableType = throwableType,
            maxStack = 1,
        )

        assertEquals(
            VerificationFrameState(
                bytecodeOffset = 8,
                locals = emptyList(),
                stack = listOf(throwableType),
            ),
            handlerFrame,
        )
    }

    @Test
    fun `handler frame enforces max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ExceptionHandlerEdgesVerifier.handlerFrame(
                handler = CodeExceptionHandler(
                    startPc = 0,
                    endPc = 5,
                    handlerPc = 8,
                    catchType = ConstantPoolIndex(7),
                ),
                exceptionStackFrame = VerificationFrameState(
                    bytecodeOffset = 4,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                throwableType = VerificationType.ObjectType(ConstantPoolIndex(9)),
                maxStack = 0,
            )
        }

        assertEquals(
            "Exception handler at bytecode offset 8 requires operand stack depth 1, exceeding max_stack=0",
            exception.message,
        )
    }

    @Test
    fun `accepts handler target when derived frame is assignable to target stack map`() {
        val handlerFrame = VerificationFrameState(
            bytecodeOffset = 8,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.ObjectType(ConstantPoolIndex(7))),
        )

        ExceptionHandlerEdgesVerifier.verifyTargetFrame(
            handlerFrame = handlerFrame,
            targetFrame = VerificationFrameState(
                bytecodeOffset = 8,
                locals = listOf(VerificationType.OneWord),
                stack = listOf(VerificationType.Reference),
            ),
        )
    }

    @Test
    fun `rejects handler target when derived stack type is not assignable to target stack map`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ExceptionHandlerEdgesVerifier.verifyTargetFrame(
                handlerFrame = VerificationFrameState(
                    bytecodeOffset = 8,
                    locals = emptyList(),
                    stack = listOf(VerificationType.ObjectType(ConstantPoolIndex(7))),
                ),
                targetFrame = VerificationFrameState(
                    bytecodeOffset = 8,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
            )
        }

        assertEquals(
            "Exception handler target 8 stack[0] contains ObjectType(constantPoolIndex=#7), expected Integer",
            exception.message,
        )
    }
}
