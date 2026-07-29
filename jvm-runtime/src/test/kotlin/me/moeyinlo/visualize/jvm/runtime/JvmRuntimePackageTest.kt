package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmRuntimePackageTest {
    @Test
    fun `runtime package name is derived from non array internal class names`() {
        assertEquals("java/lang", JvmClassDefinition(internalName = "java/lang/String").runtimePackageName())
        assertEquals("", JvmClassDefinition(internalName = "Example").runtimePackageName())
    }

    @Test
    fun `array classes do not expose a direct runtime package name`() {
        assertNull(JvmClassDefinition(internalName = "[I").runtimePackageName())
        assertNull(JvmClassDefinition(internalName = "[Ljava/lang/String;").runtimePackageName())
        assertNull(JvmClassDefinition(internalName = "[[Ljava/lang/String;").runtimePackageName())
    }

    @Test
    fun `runtime package key combines package name and defining loader`() {
        val bootstrapKey = JvmLoadedClassKey("pkg/Example", JvmClassLoaderIdentity.Bootstrap)
        val userLoader = JvmClassLoaderIdentity.UserDefined(id = 7, displayName = "plugin")
        val userKey = JvmLoadedClassKey("pkg/Example", userLoader)

        assertEquals(JvmRuntimePackageKey("pkg", JvmClassLoaderIdentity.Bootstrap), bootstrapKey.runtimePackageKey())
        assertEquals(JvmRuntimePackageKey("pkg", userLoader), userKey.runtimePackageKey())
    }

    @Test
    fun `array class keys do not expose a runtime package key`() {
        assertNull(JvmLoadedClassKey("[Ljava/lang/String;", JvmClassLoaderIdentity.Bootstrap).runtimePackageKey())
    }
}
