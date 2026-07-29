package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class JvmClassLoaderIdentityTest {
    @Test
    fun `bootstrap class loader identity is a stable singleton`() {
        assertSame(JvmClassLoaderIdentity.Bootstrap, JvmClassLoaderIdentity.Bootstrap)
        assertEquals("<bootstrap>", JvmClassLoaderIdentity.Bootstrap.displayName)
        assertEquals("bootstrap", JvmClassLoaderIdentity.Bootstrap.diagnosticName)
    }

    @Test
    fun `user defined class loader identities are distinguished by id`() {
        val first = JvmClassLoaderIdentity.UserDefined(id = 1, displayName = "app")
        val second = JvmClassLoaderIdentity.UserDefined(id = 2, displayName = "app")

        assertNotEquals(first, second)
        assertEquals("app#1", first.diagnosticName)
        assertEquals("app#2", second.diagnosticName)
    }

    @Test
    fun `class loader identities validate user defined metadata`() {
        assertFailsWith<IllegalArgumentException> {
            JvmClassLoaderIdentity.UserDefined(id = 0, displayName = "app")
        }
        assertFailsWith<IllegalArgumentException> {
            JvmClassLoaderIdentity.UserDefined(id = 1, displayName = " ")
        }
    }

    @Test
    fun `loaded class keys include defining loader identity`() {
        val bootstrapKey = JvmLoadedClassKey(
            internalName = "pkg/Example",
            definingLoader = JvmClassLoaderIdentity.Bootstrap,
        )
        val userKey = JvmLoadedClassKey(
            internalName = "pkg/Example",
            definingLoader = JvmClassLoaderIdentity.UserDefined(id = 1, displayName = "app"),
        )

        assertNotEquals(bootstrapKey, userKey)
        assertEquals("pkg/Example @ bootstrap", bootstrapKey.diagnosticName)
        assertEquals("pkg/Example @ app#1", userKey.diagnosticName)
    }

    @Test
    fun `loaded class keys reject blank class names`() {
        assertFailsWith<IllegalArgumentException> {
            JvmLoadedClassKey("", JvmClassLoaderIdentity.Bootstrap)
        }
    }
}
