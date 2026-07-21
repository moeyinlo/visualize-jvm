package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmMethodDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
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

    @Test
    fun `GetStaticMethodID returns a method handle for loaded static guest methods`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "answer",
                                descriptor = "()I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")

        val methodHandle = environment.getStaticMethodId(classHandle, "answer", "()I")

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
                isStatic = true,
            ),
            handles.resolveMethodId(methodHandle),
        )
    }

    @Test
    fun `GetStaticMethodID throws guest NoSuchMethodError for missing or non static methods`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "instanceOnly",
                                descriptor = "()I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val classHandle = environment.findClass("Example")

        assertFailsWith<JvmNoSuchMethodError> {
            environment.getStaticMethodId(classHandle, "missing", "()I")
        }
        assertFailsWith<JvmNoSuchMethodError> {
            environment.getStaticMethodId(classHandle, "instanceOnly", "()I")
        }
    }
}
