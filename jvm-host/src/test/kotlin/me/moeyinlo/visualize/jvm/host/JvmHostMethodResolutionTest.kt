package me.moeyinlo.visualize.jvm.host

import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmHostMethodResolutionTest {
    @Test
    fun `resolves public instance host methods from JVM descriptors`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(String::class.java)
        val method = JvmHostMethodResolver.resolveInstanceMethod(
            owner = mirror,
            name = "substring",
            descriptor = "(I)Ljava/lang/String;",
        )

        assertEquals("substring", method.name)
        assertEquals("(I)Ljava/lang/String;", method.descriptor)
        assertEquals(String::class.java.getMethod("substring", Int::class.javaPrimitiveType!!), method.hostMethod)
        assertFalse(method.isStatic)
        assertEquals(String::class.java, method.returnType)
        assertEquals(listOf<Class<*>>(Int::class.javaPrimitiveType!!), method.parameterTypes)
    }

    @Test
    fun `resolves public static host methods from JVM descriptors`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(Int::class.javaObjectType)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "valueOf",
            descriptor = "(I)Ljava/lang/Integer;",
        )

        assertTrue(Modifier.isStatic(method.hostMethod.modifiers))
        assertTrue(method.isStatic)
        assertEquals(Int::class.javaObjectType, method.returnType)
    }

    @Test
    fun `rejects static resolution for instance host methods`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(String::class.java)

        val exception = assertFailsWith<JvmHostMethodResolutionException> {
            JvmHostMethodResolver.resolveStaticMethod(
                owner = mirror,
                name = "substring",
                descriptor = "(I)Ljava/lang/String;",
            )
        }

        assertEquals("Host method java.lang.String.substring:(I)Ljava/lang/String; is not static", exception.message)
    }
}
