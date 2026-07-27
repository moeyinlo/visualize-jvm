package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmMethodDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmSimulatedJniRegisteredNativesTest {
    @Test
    fun `RegisterNatives records methods for the supplied guest class handle`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/NativeApi",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "a",
                                descriptor = "()V",
                                isStatic = true,
                                isNative = true,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val classHandle = environment.findClass("pkg/NativeApi")

        assertEquals(
            0,
            environment.registerNatives(
                classHandle = classHandle,
                methods = listOf(JvmJniNativeMethodDescriptor("a", "()V", 0x1234L)),
            ),
        )

        assertEquals(
            JvmJniRegisteredNativeMethod("pkg/NativeApi", "a", "()V", 0x1234L),
            environment.registeredNativeMethods.resolve("pkg/NativeApi", "a", "()V"),
        )
    }

    @Test
    fun `UnregisterNatives removes methods for the supplied guest class handle`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/NativeApi",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "a",
                                descriptor = "()V",
                                isStatic = true,
                                isNative = true,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val classHandle = environment.findClass("pkg/NativeApi")
        environment.registerNatives(
            classHandle = classHandle,
            methods = listOf(JvmJniNativeMethodDescriptor("a", "()V", 0x1234L)),
        )

        assertEquals(0, environment.unregisterNatives(classHandle))

        assertEquals(null, environment.registeredNativeMethods.resolve("pkg/NativeApi", "a", "()V"))
    }

    @Test
    fun `RegisterNatives rejects methods that are not declared native by the supplied class`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/NativeApi",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "ordinary",
                                descriptor = "()V",
                                isStatic = true,
                                isNative = false,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val classHandle = environment.findClass("pkg/NativeApi")

        val exception = assertFailsWith<JvmNoSuchMethodError> {
            environment.registerNatives(
                classHandle = classHandle,
                methods = listOf(JvmJniNativeMethodDescriptor("ordinary", "()V", 0x1234L)),
            )
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("pkg/NativeApi.ordinary:()V", exception.message)
        assertEquals(null, environment.registeredNativeMethods.resolve("pkg/NativeApi", "ordinary", "()V"))
    }

    @Test
    fun `RegisterNatives rejects class handles whose classes are not loaded`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            handles = handles,
        )
        val missingClassHandle = handles.newClassHandle("pkg/Missing")

        val exception = assertFailsWith<JvmNoClassDefFoundError> {
            environment.registerNatives(
                classHandle = missingClassHandle,
                methods = listOf(JvmJniNativeMethodDescriptor("a", "()V", 0x1234L)),
            )
        }

        assertEquals("java/lang/NoClassDefFoundError", exception.guestClassName)
        assertEquals("pkg/Missing", exception.message)
    }
}
