package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NestmateAccessVerifierTest {
    @Test
    fun `classes with the same runtime nest host and loader are nestmates`() {
        NestmateAccessVerifier.verify(
            access = NestmateAccess(
                currentClass = nestClass("pkg/Outer\$Inner", nestHost = "pkg/Outer"),
                targetClass = nestClass("pkg/Outer", nestHost = "pkg/Outer"),
                memberName = "secret",
                descriptor = "I",
            ),
        )
    }

    @Test
    fun `same named nest host from different defining loaders is not a nestmate`() {
        val exception = assertFailsWith<MethodVerificationException> {
            NestmateAccessVerifier.verify(
                access = NestmateAccess(
                    currentClass = nestClass("pkg/Outer\$Inner", nestHost = "pkg/Outer", loader = "app"),
                    targetClass = nestClass("pkg/Outer", nestHost = "pkg/Outer", loader = "bootstrap"),
                    memberName = "secret",
                    descriptor = "I",
                ),
            )
        }

        assertEquals(
            "Nestmate access to pkg/Outer.secret:I from pkg/Outer\$Inner requires the same runtime nest host",
            exception.message,
        )
    }

    @Test
    fun `different runtime nest hosts are not nestmates`() {
        val exception = assertFailsWith<MethodVerificationException> {
            NestmateAccessVerifier.verify(
                access = NestmateAccess(
                    currentClass = nestClass("pkg/Outer\$Inner", nestHost = "pkg/Outer"),
                    targetClass = nestClass("pkg/Other", nestHost = "pkg/Other"),
                    memberName = "secret",
                    descriptor = "I",
                ),
            )
        }

        assertEquals(
            "Nestmate access to pkg/Other.secret:I from pkg/Outer\$Inner requires the same runtime nest host",
            exception.message,
        )
    }

    private fun nestClass(
        internalName: String,
        nestHost: String,
        loader: String = "bootstrap",
    ): NestmateVerifierClass =
        NestmateVerifierClass(
            internalName = internalName,
            definingLoader = loader,
            nestHostInternalName = nestHost,
        )
}
