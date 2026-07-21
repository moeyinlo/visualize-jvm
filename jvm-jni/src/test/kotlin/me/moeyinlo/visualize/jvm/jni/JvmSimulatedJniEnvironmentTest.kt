package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmSimulatedJniEnvironmentTest {
    @Test
    fun `FindClass returns a class handle for loaded guest classes`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            handles = handles,
        )

        val classHandle = environment.findClass("Example")

        assertEquals("Example", handles.resolveClass(classHandle))
    }

    @Test
    fun `FindClass throws guest NoClassDefFoundError when the class is not loaded`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        val exception = assertFailsWith<JvmNoClassDefFoundError> {
            environment.findClass("Missing")
        }

        assertEquals("java/lang/NoClassDefFoundError", exception.guestClassName)
        assertEquals("Missing", exception.message)
    }
}
