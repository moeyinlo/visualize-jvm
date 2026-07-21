package me.moeyinlo.visualize.jvm.jni

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmNativeSymbolNameResolverTest {
    @Test
    fun `resolver creates short JNI symbols with escaped class and method names`() {
        val signature = JvmNativeGuestMethodSignature(
            ownerClassName = "pkg/Native_Api",
            methodName = "call_native",
            methodDescriptor = "()V",
            isStatic = true,
        )

        assertEquals(
            "Java_pkg_Native_1Api_call_1native",
            JvmNativeSymbolNameResolver.shortName(signature),
        )
    }

    @Test
    fun `resolver creates long JNI symbols with escaped parameter descriptors`() {
        val signature = JvmNativeGuestMethodSignature(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "(ILjava/lang/String;[I)V",
            isStatic = false,
        )

        assertEquals(
            "Java_pkg_NativeApi_call__ILjava_lang_String_2_3I",
            JvmNativeSymbolNameResolver.longName(signature),
        )
        assertEquals(
            listOf(
                "Java_pkg_NativeApi_call",
                "Java_pkg_NativeApi_call__ILjava_lang_String_2_3I",
            ),
            JvmNativeSymbolNameResolver.candidates(signature),
        )
    }

    @Test
    fun `resolver rejects invalid method descriptors`() {
        val signature = JvmNativeGuestMethodSignature(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "I)V",
            isStatic = false,
        )

        assertFailsWith<IllegalArgumentException> {
            JvmNativeSymbolNameResolver.longName(signature)
        }
    }
}
