package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.verifier.JvmClassVerificationException as VerifierClassVerificationException

class JvmVerifierLinkingVerifierTest {
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
