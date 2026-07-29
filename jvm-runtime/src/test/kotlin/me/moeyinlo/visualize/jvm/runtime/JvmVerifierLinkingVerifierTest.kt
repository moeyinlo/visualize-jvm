package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.FullStackMapFrame
import me.moeyinlo.visualize.jvm.classfile.VerificationTypeInfo
import me.moeyinlo.visualize.jvm.verifier.JvmClassVerificationException as VerifierClassVerificationException

class JvmVerifierLinkingVerifierTest {
    @Test
    fun `concrete verifier adapter consumes runtime StackMapTable frames`() {
        val linker = JvmClassLinker(verifier = JvmVerifierLinkingVerifier())

        val failure = assertFailsWith<JvmClassVerificationException> {
            linker.link(
                JvmClassDefinition(
                    internalName = "pkg/BadStackMap",
                    majorVersion = 70,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "badFrame",
                            descriptor = "()V",
                            isStatic = true,
                            code = byteArrayOf(0xB1.toByte()),
                            maxStack = 0,
                            maxLocals = 1,
                            hasStackMapTable = true,
                            stackMapTableEntries = listOf(
                                FullStackMapFrame(
                                    offsetDelta = 0,
                                    locals = listOf(VerificationTypeInfo.Long),
                                    stack = emptyList(),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }

        val verifierFailure = failure.cause as VerifierClassVerificationException
        assertEquals("badFrame()V", verifierFailure.methodSignature)
    }

    @Test
    fun `concrete verifier adapter rejects classes before linker records them`() {
        val linker = JvmClassLinker(verifier = JvmVerifierLinkingVerifier())

        val failure = assertFailsWith<JvmClassVerificationException> {
            linker.link(
                JvmClassDefinition(
                    internalName = "pkg/Bad",
                    majorVersion = 70,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "tooManyLocals",
                            descriptor = "(J)V",
                            isStatic = true,
                            code = byteArrayOf(0xB1.toByte()),
                            maxStack = 0,
                            maxLocals = 1,
                        ),
                    ),
                ),
            )
        }

        assertEquals("pkg/Bad", failure.className)
        assertEquals("java/lang/VerifyError", failure.guestThrowableClassName)
        assertEquals(null, linker.linkedClass("pkg/Bad"))
        val verifierFailure = failure.cause as VerifierClassVerificationException
        assertEquals("tooManyLocals(J)V", verifierFailure.methodSignature)
    }
}
