package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class JvmSimulatedJavaVmFunctionTableTest {
    @Test
    fun `JavaVM function table delegates GetEnv to one simulated JavaVM`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            staticFields = JvmStaticFields(),
        )
        val javaVm = JvmSimulatedJavaVm(environment)
        val functions = javaVm.functions

        val ok = functions.getEnv(JvmJniVersions.Version24)
        val unsupported = functions.getEnv(0x7fff0000)

        assertEquals(JvmJniStatus.Ok, ok.status)
        assertSame(environment, ok.environment)
        assertEquals(JvmJniStatus.EVersion, unsupported.status)
        assertEquals(null, unsupported.environment)
    }
    @Test
    fun `JavaVM function table delegates AttachCurrentThread and DetachCurrentThread`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            staticFields = JvmStaticFields(),
        )
        val javaVm = JvmSimulatedJavaVm(environment)
        val functions = javaVm.functions

        assertEquals(JvmJniStatus.Ok, functions.detachCurrentThread())
        assertEquals(JvmJniStatus.EDetached, functions.getEnv(JvmJniVersions.Version24).status)

        val attached = functions.attachCurrentThread(JvmJniVersions.Version24)

        assertEquals(JvmJniStatus.Ok, attached.status)
        assertSame(environment, attached.environment)
    }
    @Test
    fun `JavaVM function table delegates AttachCurrentThreadAsDaemon`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            staticFields = JvmStaticFields(),
        )
        val javaVm = JvmSimulatedJavaVm(environment)
        val functions = javaVm.functions

        assertEquals(JvmJniStatus.Ok, functions.detachCurrentThread())
        val attached = functions.attachCurrentThreadAsDaemon(JvmJniVersions.Version24)

        assertEquals(JvmJniStatus.Ok, attached.status)
        assertSame(environment, attached.environment)
        assertEquals(true, javaVm.isCurrentThreadDaemonAttached)
    }
    @Test
    fun `JavaVM function table delegates DestroyJavaVM`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            staticFields = JvmStaticFields(),
        )
        val javaVm = JvmSimulatedJavaVm(environment)
        val functions = javaVm.functions

        assertEquals(JvmJniStatus.Ok, functions.destroyJavaVm())

        assertEquals(true, javaVm.isDestroyed)
        assertEquals(JvmJniStatus.EDetached, functions.getEnv(JvmJniVersions.Version24).status)
    }
}
