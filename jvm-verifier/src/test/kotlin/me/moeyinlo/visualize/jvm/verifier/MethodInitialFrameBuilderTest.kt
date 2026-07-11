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
