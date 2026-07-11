package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MethodInitialFrameBuilderTest {
    @Test
    fun `builds static method initial frame from descriptor arguments`() {
        assertEquals(
            MethodInitialFrame(
                locals = listOf(
                    VerificationType.Integer,
                    VerificationType.Long,
                    VerificationType.Top,
                    VerificationType.ClassType("java/lang/String"),
                    VerificationType.Top,
                ),
                stack = emptyList(),
                flags = emptyList(),
                returnType = VerificationType.ClassType("java/lang/Object"),
            ),
            MethodInitialFrameBuilder.buildStatic(
                descriptor = "(IJLjava/lang/String;)Ljava/lang/Object;",
                maxLocals = 5,
            ),
        )
    }

    @Test
    fun `builds instance method initial frame with class this local`() {
        assertEquals(
            MethodInitialFrame(
                locals = listOf(
                    VerificationType.ClassType("example/Foo", loader = "app"),
                    VerificationType.Integer,
                    VerificationType.Long,
                    VerificationType.Top,
                    VerificationType.Top,
                ),
                stack = emptyList(),
                flags = emptyList(),
                returnType = VerificationType.Integer,
            ),
            MethodInitialFrameBuilder.buildInstance(
                currentClass = "example/Foo",
                currentClassLoader = "app",
                descriptor = "(IJ)I",
                maxLocals = 5,
            ),
        )
    }

    @Test
    fun `builds subclass constructor initial frame with uninitialized this`() {
        assertEquals(
            MethodInitialFrame(
                locals = listOf(
                    VerificationType.UninitializedThis,
                    VerificationType.Integer,
                    VerificationType.Top,
                ),
                stack = emptyList(),
                flags = listOf(MethodInitialFrameFlag.ThisUninitialized),
                returnType = null,
            ),
            MethodInitialFrameBuilder.buildSubclassConstructor(
                descriptor = "(I)V",
                maxLocals = 3,
            ),
        )
    }

    @Test
    fun `builds Object constructor initial frame with class this local`() {
        assertEquals(
            MethodInitialFrame(
                locals = listOf(
                    VerificationType.ClassType("java/lang/Object", loader = "bootstrap"),
                    VerificationType.Top,
                ),
                stack = emptyList(),
                flags = emptyList(),
                returnType = null,
            ),
            MethodInitialFrameBuilder.buildObjectConstructor(
                descriptor = "()V",
                maxLocals = 2,
            ),
        )
    }

    @Test
    fun `rejects static method initial frame when descriptor arguments exceed max locals`() {
        val exception =
            assertFailsWith<MethodVerificationException> {
                MethodInitialFrameBuilder.buildStatic(
                    descriptor = "(JD)V",
                    maxLocals = 3,
                )
            }

        assertEquals(
            "Initial frame locals use 4 local variable unit(s), exceeding max_locals=3",
            exception.message,
        )
    }
}
