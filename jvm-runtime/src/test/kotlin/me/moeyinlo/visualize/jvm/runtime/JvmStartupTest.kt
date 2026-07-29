package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmStartupTest {
    @Test
    fun `startup request validates main class names`() {
        assertFailsWith<IllegalArgumentException> {
            JvmStartupRequest(mainClassName = " ")
        }
    }

    @Test
    fun `startup resolves public static main with String array descriptor`() {
        val methodArea = JvmMethodArea()
        val mainMethod = JvmMethodDefinition(
            name = "main",
            descriptor = "([Ljava/lang/String;)V",
            isStatic = true,
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(0xB1.toByte()),
        )
        methodArea.defineClass(
            JvmMethodAreaEntry(
                JvmClassDefinition(
                    internalName = "pkg/Main",
                    methods = listOf(
                        JvmMethodDefinition(name = "main", descriptor = "()V", isStatic = true),
                        mainMethod,
                    ),
                ),
            ),
        )

        val entryPoint = JvmStartupResolver.resolveMainMethod(
            methodArea = methodArea,
            request = JvmStartupRequest(mainClassName = "pkg/Main", arguments = listOf("a", "b")),
        )

        assertEquals("pkg/Main", entryPoint.className)
        assertEquals(listOf("a", "b"), entryPoint.arguments)
        assertEquals(mainMethod, entryPoint.method)
    }

    @Test
    fun `startup rejects missing main class with guest NoClassDefFoundError identity`() {
        val failure = assertFailsWith<JvmStartupException> {
            JvmStartupResolver.resolveMainMethod(
                methodArea = JvmMethodArea(),
                request = JvmStartupRequest(mainClassName = "pkg/Missing"),
            )
        }

        assertEquals("java/lang/NoClassDefFoundError", failure.guestThrowableClassName)
        assertEquals("pkg/Missing", failure.className)
    }

    @Test
    fun `startup rejects non public static main candidates with guest NoSuchMethodError identity`() {
        val methodArea = JvmMethodArea()
        methodArea.defineClass(
            JvmMethodAreaEntry(
                JvmClassDefinition(
                    internalName = "pkg/Main",
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "main",
                            descriptor = "([Ljava/lang/String;)V",
                            isStatic = false,
                        ),
                        JvmMethodDefinition(
                            name = "main",
                            descriptor = "([Ljava/lang/String;)V",
                            isStatic = true,
                            isPrivate = true,
                        ),
                    ),
                ),
            ),
        )

        val failure = assertFailsWith<JvmStartupException> {
            JvmStartupResolver.resolveMainMethod(methodArea, JvmStartupRequest("pkg/Main"))
        }

        assertEquals("java/lang/NoSuchMethodError", failure.guestThrowableClassName)
        assertEquals("pkg/Main.main:([Ljava/lang/String;)V", failure.message)
    }
}
