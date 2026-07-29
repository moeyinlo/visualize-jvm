package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JvmModuleLayerTest {
    @Test
    fun `module descriptors validate names packages and requires`() {
        assertFailsWith<IllegalArgumentException> {
            JvmModuleDescriptor(name = " ", packages = setOf("pkg"))
        }
        assertFailsWith<IllegalArgumentException> {
            JvmModuleDescriptor(name = "app", packages = setOf(""))
        }
        assertFailsWith<IllegalArgumentException> {
            JvmModuleDescriptor(name = "app", requires = setOf(" "))
        }
    }

    @Test
    fun `module layer defines and finds modules with parent fallback`() {
        val javaBase = JvmModuleDescriptor(name = "java.base", packages = setOf("java/lang"))
        val parent = JvmModuleLayer(parent = null).define(javaBase)
        val app = JvmModuleDescriptor(name = "app", packages = setOf("app"), requires = setOf("java.base"))
        val child = JvmModuleLayer(parent = parent).define(app)

        assertSame(app, child.findModule("app"))
        assertSame(javaBase, child.findModule("java.base"))
        assertEquals(listOf("app"), child.modules().map(JvmModuleDescriptor::name))
    }

    @Test
    fun `module layer rejects duplicate module names in one layer`() {
        val layer = JvmModuleLayer(parent = null)
            .define(JvmModuleDescriptor(name = "app", packages = setOf("app")))

        val failure = assertFailsWith<JvmModuleLayerException> {
            layer.define(JvmModuleDescriptor(name = "app", packages = setOf("other")))
        }

        assertEquals("Module app is already defined in this layer", failure.message)
    }

    @Test
    fun `module layer rejects split packages in one layer`() {
        val layer = JvmModuleLayer(parent = null)
            .define(JvmModuleDescriptor(name = "first", packages = setOf("shared")))

        val failure = assertFailsWith<JvmModuleLayerException> {
            layer.define(JvmModuleDescriptor(name = "second", packages = setOf("shared")))
        }

        assertEquals("Package shared is already defined by module first in this layer", failure.message)
    }

    @Test
    fun `module readability includes self and declared requires`() {
        val layer = JvmModuleLayer(parent = null)
            .define(JvmModuleDescriptor(name = "java.base", packages = setOf("java/lang")))
            .define(JvmModuleDescriptor(name = "lib", packages = setOf("lib")))
            .define(JvmModuleDescriptor(name = "app", packages = setOf("app"), requires = setOf("java.base")))

        assertTrue(layer.canRead("app", "app"))
        assertTrue(layer.canRead("app", "java.base"))
        assertFalse(layer.canRead("app", "lib"))
    }

    @Test
    fun `module readability reports missing modules`() {
        val layer = JvmModuleLayer(parent = null)
            .define(JvmModuleDescriptor(name = "app", packages = setOf("app")))

        val failure = assertFailsWith<JvmModuleLayerException> {
            layer.canRead("app", "missing")
        }

        assertEquals("Module missing is not defined in this layer graph", failure.message)
    }
    @Test
    fun `module layer finds package owners with parent fallback`() {
        val parent = JvmModuleLayer(parent = null)
            .define(JvmModuleDescriptor(name = "java.base", packages = setOf("java/lang")))
        val child = JvmModuleLayer(parent = parent)
            .define(JvmModuleDescriptor(name = "app", packages = setOf("app")))

        assertEquals("app", child.findPackageOwner("app")?.name)
        assertEquals("java.base", child.findPackageOwner("java/lang")?.name)
        assertEquals(null, child.findPackageOwner("missing"))
    }

    @Test
    fun `package owner lookup rejects blank package names`() {
        val layer = JvmModuleLayer(parent = null)

        val failure = assertFailsWith<IllegalArgumentException> {
            layer.findPackageOwner("")
        }

        assertEquals("package name must not be blank", failure.message)
    }
}
