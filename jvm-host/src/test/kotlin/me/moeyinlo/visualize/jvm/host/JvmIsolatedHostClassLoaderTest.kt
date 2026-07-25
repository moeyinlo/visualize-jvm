package me.moeyinlo.visualize.jvm.host

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmIsolatedHostClassLoaderTest {
    @Test
    fun `loads a platform class mirror by guest internal name`() {
        val loader = JvmIsolatedHostClassLoader()

        val mirror = loader.loadClassMirror("java/lang/String")

        assertEquals("java/lang/String", mirror.guestInternalName)
        assertEquals(String::class.java, mirror.hostClass)
    }

    @Test
    fun `reports missing host classes with the guest internal name`() {
        val loader = JvmIsolatedHostClassLoader()

        val exception = assertFailsWith<JvmHostClassLoadingException> {
            loader.loadClassMirror("missing/HostClass")
        }

        assertEquals("Host class missing.HostClass for missing/HostClass was not found", exception.message)
    }
}
