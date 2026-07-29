package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class JvmClassVerifierTest {
    @Test
    fun `class verifier selects strategy from version and stack map presence`() {
        val verifier = JvmClassVerifier()

        val result = verifier.verify(
            JvmClassVerificationRequest(
                className = "pkg/Example",
                majorVersion = 70,
                methods = listOf(
                    JvmMethodVerificationRequest(
                        name = "main",
                        descriptor = "([Ljava/lang/String;)V",
                        isStatic = true,
                        code = returningCode(maxStack = 0, maxLocals = 1),
                        hasStackMapTable = true,
                    ),
                ),
            ),
        )

        assertEquals("pkg/Example", result.className)
        assertEquals(JvmVerificationStrategy.TypeChecking, result.strategy)
        assertEquals(listOf("main([Ljava/lang/String;)V"), result.verifiedMethodSignatures)
    }

    @Test
    fun `class verifier rejects initial frame locals beyond max locals`() {
        val verifier = JvmClassVerifier()

        val failure = assertFailsWith<JvmClassVerificationException> {
            verifier.verify(
                JvmClassVerificationRequest(
                    className = "pkg/Bad",
                    majorVersion = 70,
                    methods = listOf(
                        JvmMethodVerificationRequest(
                            name = "tooManyLocals",
                            descriptor = "(J)V",
                            isStatic = true,
                            code = returningCode(maxStack = 0, maxLocals = 1),
                        ),
                    ),
                ),
            )
        }

        assertEquals("pkg/Bad", failure.className)
        assertEquals("tooManyLocals(J)V", failure.methodSignature)
    }

    @Test
    fun `class verifier skips native and abstract methods without Code`() {
        val verifier = JvmClassVerifier()

        val result = verifier.verify(
            JvmClassVerificationRequest(
                className = "pkg/NativeShape",
                majorVersion = 49,
                methods = listOf(
                    JvmMethodVerificationRequest(
                        name = "nativeCall",
                        descriptor = "()V",
                        isNative = true,
                    ),
                    JvmMethodVerificationRequest(
                        name = "abstractCall",
                        descriptor = "()V",
                        isAbstract = true,
                    ),
                ),
            ),
        )

        assertEquals(JvmVerificationStrategy.TypeInference, result.strategy)
        assertEquals(emptyList(), result.verifiedMethodSignatures)
    }

    private fun returningCode(maxStack: Int, maxLocals: Int): CodeAttribute =
        CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = maxStack,
            maxLocals = maxLocals,
            code = byteArrayOf(0xB1.toByte()),
        )
}
