package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class JvmSimulatedJavaVmTest {
    @Test
    fun `DetachCurrentThread marks GetEnv as detached until AttachCurrentThread reattaches`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            staticFields = JvmStaticFields(),
        )
        val javaVm = JvmSimulatedJavaVm(environment)

        assertEquals(JvmJniStatus.Ok, javaVm.detachCurrentThread())
        val detached = javaVm.getEnv(JvmJniVersions.Version24)
        assertEquals(JvmJniStatus.EDetached, detached.status)
        assertEquals(null, detached.environment)

        val attached = javaVm.attachCurrentThread(JvmJniVersions.Version24)
        assertEquals(JvmJniStatus.Ok, attached.status)
        assertSame(environment, attached.environment)
        assertSame(environment, javaVm.getEnv(JvmJniVersions.Version24).environment)
    }

    @Test
    fun `AttachCurrentThread rejects unsupported requested JNI versions`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            staticFields = JvmStaticFields(),
        )
        val javaVm = JvmSimulatedJavaVm(environment)

        val result = javaVm.attachCurrentThread(0x7fff0000)

        assertEquals(JvmJniStatus.EVersion, result.status)
        assertEquals(null, result.environment)
    }
}
