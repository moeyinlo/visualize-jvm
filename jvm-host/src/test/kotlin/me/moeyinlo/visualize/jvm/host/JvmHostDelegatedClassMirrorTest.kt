package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmClassExecutionMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmHostDelegatedClassMirrorTest {
    @Test
    fun `mirror records guest internal name and host class as an opaque host delegated boundary`() {
        val mirror = JvmHostDelegatedClassMirror(
            guestInternalName = "java/lang/String",
            hostClass = String::class.java,
        )

        assertEquals("java/lang/String", mirror.guestInternalName)
        assertEquals("java.lang.String", mirror.hostBinaryName)
        assertEquals(String::class.java, mirror.hostClass)
        assertEquals(JvmClassExecutionMode.HostDelegated, mirror.executionMode)
    }

    @Test
    fun `mirror can be derived from a host class`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(java.util.ArrayList::class.java)

        assertEquals("java/util/ArrayList", mirror.guestInternalName)
        assertEquals("java.util.ArrayList", mirror.hostBinaryName)
    }

    @Test
    fun `mirror rejects internal names that do not match the host class binary name`() {
        val exception = assertFailsWith<JvmHostMirrorMismatchException> {
            JvmHostDelegatedClassMirror(
                guestInternalName = "java/lang/Integer",
                hostClass = String::class.java,
            )
        }

        assertEquals(
            "Host class java.lang.String does not match guest internal name java/lang/Integer",
            exception.message,
        )
    }
}
