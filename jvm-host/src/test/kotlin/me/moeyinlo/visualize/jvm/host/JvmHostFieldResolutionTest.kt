package me.moeyinlo.visualize.jvm.host

import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmHostFieldResolutionTest {
    @Test
    fun `resolves public instance host fields from JVM descriptors`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(PublicHostFieldFixture::class.java)
        val field = JvmHostFieldResolver.resolveInstanceField(
            owner = mirror,
            name = "instanceCount",
            descriptor = "I",
        )

        assertEquals("instanceCount", field.name)
        assertEquals("I", field.descriptor)
        assertEquals(PublicHostFieldFixture::class.java.getField("instanceCount"), field.hostField)
        assertFalse(field.isStatic)
        assertEquals(Int::class.javaPrimitiveType!!, field.fieldType)
    }

    @Test
    fun `resolves public static host fields from JVM descriptors`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(Int::class.javaObjectType)
        val field = JvmHostFieldResolver.resolveStaticField(
            owner = mirror,
            name = "MAX_VALUE",
            descriptor = "I",
        )

        assertTrue(Modifier.isStatic(field.hostField.modifiers))
        assertTrue(field.isStatic)
        assertEquals(Int::class.javaPrimitiveType!!, field.fieldType)
    }

    @Test
    fun `rejects static resolution for instance host fields`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(PublicHostFieldFixture::class.java)

        val exception = assertFailsWith<JvmHostFieldResolutionException> {
            JvmHostFieldResolver.resolveStaticField(
                owner = mirror,
                name = "instanceCount",
                descriptor = "I",
            )
        }

        assertEquals(
            "Host field me.moeyinlo.visualize.jvm.host.JvmHostFieldResolutionTest\$PublicHostFieldFixture.instanceCount:I is not static",
            exception.message,
        )
    }

    @Test
    fun `rejects host fields with mismatched descriptors`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(PublicHostFieldFixture::class.java)

        val exception = assertFailsWith<JvmHostFieldResolutionException> {
            JvmHostFieldResolver.resolveInstanceField(
                owner = mirror,
                name = "instanceCount",
                descriptor = "J",
            )
        }

        assertEquals(
            "Host field me.moeyinlo.visualize.jvm.host.JvmHostFieldResolutionTest\$PublicHostFieldFixture.instanceCount:J has type int, expected long",
            exception.message,
        )
    }

    class PublicHostFieldFixture {
        @JvmField
        var instanceCount: Int = 0
    }
}
