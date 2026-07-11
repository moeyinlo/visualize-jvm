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

    @Test
    fun `parses array method parameters as nested array verification types`() {
        assertEquals(
            listOf(
                VerificationType.ArrayOf(VerificationType.Integer),
                VerificationType.ArrayOf(
                    VerificationType.ArrayOf(VerificationType.ClassType("java/lang/String")),
                ),
                VerificationType.ArrayOf(VerificationType.Byte),
                VerificationType.ArrayOf(
                    VerificationType.ArrayOf(VerificationType.Boolean),
                ),
            ),
            MethodDescriptorVerificationTypeParser.parseParameterTypes("([I[[Ljava/lang/String;[B[[Z)V"),
        )
    }

    @Test
    fun `exposes method return verification type`() {
        assertEquals(
            MethodDescriptorVerificationTypes(
                parameterTypes = listOf(VerificationType.Integer),
                returnType = VerificationType.ClassType("java/lang/String"),
            ),
            MethodDescriptorVerificationTypeParser.parse("(I)Ljava/lang/String;"),
        )
    }

    @Test
    fun `exposes void method return as absent verification type`() {
        assertEquals(
            MethodDescriptorVerificationTypes(
                parameterTypes = listOf(VerificationType.Integer),
                returnType = null,
            ),
            MethodDescriptorVerificationTypeParser.parse("(I)V"),
        )
    }
}
