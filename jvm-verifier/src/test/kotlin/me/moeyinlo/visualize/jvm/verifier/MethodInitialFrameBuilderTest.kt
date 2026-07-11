package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
