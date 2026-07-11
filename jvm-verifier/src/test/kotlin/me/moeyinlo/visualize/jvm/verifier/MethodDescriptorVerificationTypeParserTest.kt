package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals

class MethodDescriptorVerificationTypeParserTest {
    @Test
    fun `parses empty method parameter list`() {
        assertEquals(
            emptyList(),
            MethodDescriptorVerificationTypeParser.parseParameterTypes("()V"),
        )
    }

    @Test
    fun `parses primitive method parameters as verification types`() {
        assertEquals(
            listOf(
                VerificationType.Integer,
                VerificationType.Long,
                VerificationType.Float,
                VerificationType.Double,
                VerificationType.Integer,
                VerificationType.Integer,
                VerificationType.Integer,
                VerificationType.Integer,
            ),
            MethodDescriptorVerificationTypeParser.parseParameterTypes("(IJFDZBCS)V"),
        )
    }

    @Test
    fun `parses class method parameters as loaded class verification types`() {
        assertEquals(
            listOf(
                VerificationType.ClassType("java/lang/String"),
                VerificationType.ClassType("java/util/List"),
            ),
            MethodDescriptorVerificationTypeParser.parseParameterTypes(
                "(Ljava/lang/String;Ljava/util/List;)V",
            ),
        )
    }
}
