package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class JvmClassLinkerTest {
    @Test
    fun `linking verifies classes before recording linked state`() {
        val definition = JvmClassDefinition(internalName = "pkg/Example")
        val verified = mutableListOf<String>()
        val linker = JvmClassLinker(
            verifier = JvmLinkingVerifier { candidate ->
                verified += candidate.internalName
            },
        )

        val linked = linker.link(definition)

        assertEquals(listOf("pkg/Example"), verified)
        assertEquals(JvmClassLinkState.Verified, linked.state)
        assertSame(linked, linker.linkedClass("pkg/Example"))
    }

    @Test
    fun `linking verification failures expose guest VerifyError identity`() {
        val verifierFailure = IllegalStateException("bad stack map")
        val linker = JvmClassLinker(
            verifier = JvmLinkingVerifier {
                throw verifierFailure
            },
        )

        val failure = assertFailsWith<JvmClassVerificationException> {
            linker.link(JvmClassDefinition(internalName = "pkg/Bad"))
        }

        assertEquals("pkg/Bad", failure.className)
        assertEquals("java/lang/VerifyError", failure.guestThrowableClassName)
        assertSame(verifierFailure, failure.cause)
        assertEquals(null, linker.linkedClass("pkg/Bad"))
    }

    @Test
    fun `linking rejects duplicate linked class definitions`() {
        val linker = JvmClassLinker(verifier = JvmLinkingVerifier.NoOp)
        linker.link(JvmClassDefinition(internalName = "pkg/Example"))

        val failure = assertFailsWith<JvmClassLinkageException> {
            linker.link(JvmClassDefinition(internalName = "pkg/Example"))
        }

        assertEquals("Class pkg/Example is already linked", failure.message)
    }

    @Test
    fun `linking validates class names`() {
        val linker = JvmClassLinker(verifier = JvmLinkingVerifier.NoOp)

        assertFailsWith<IllegalArgumentException> {
            linker.link(JvmClassDefinition(internalName = ""))
        }
        assertFailsWith<IllegalArgumentException> {
            linker.linkedClass("")
        }
    }
}
