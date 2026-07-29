package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmLoadingConstraintSetTest {
    @Test
    fun `loading constraints merge loader sets transitively per class name`() {
        val constraints = JvmLoadingConstraintSet()
        val app = JvmClassLoaderIdentity.UserDefined(id = 1, displayName = "app")
        val plugin = JvmClassLoaderIdentity.UserDefined(id = 2, displayName = "plugin")
        val child = JvmClassLoaderIdentity.UserDefined(id = 3, displayName = "child")

        constraints.addConstraint("pkg/Foo", app, plugin)
        constraints.addConstraint("pkg/Foo", plugin, child)

        assertEquals(
            setOf(app, plugin, child),
            constraints.constrainedLoaders("pkg/Foo", app),
        )
        assertEquals(
            setOf(JvmClassLoaderIdentity.Bootstrap),
            constraints.constrainedLoaders("pkg/Other", JvmClassLoaderIdentity.Bootstrap),
        )
    }

    @Test
    fun `recorded resolutions are shared across constrained loaders`() {
        val constraints = JvmLoadingConstraintSet()
        val app = JvmClassLoaderIdentity.UserDefined(id = 1, displayName = "app")
        val plugin = JvmClassLoaderIdentity.UserDefined(id = 2, displayName = "plugin")
        val resolved = JvmLoadedClassKey("pkg/Foo", JvmClassLoaderIdentity.Bootstrap)

        constraints.addConstraint("pkg/Foo", app, plugin)
        constraints.recordResolution("pkg/Foo", app, resolved)

        assertEquals(resolved, constraints.resolvedClass("pkg/Foo", app))
        assertEquals(resolved, constraints.resolvedClass("pkg/Foo", plugin))
        constraints.recordResolution("pkg/Foo", plugin, resolved)
    }

    @Test
    fun `recording an incompatible constrained resolution reports a guest LinkageError identity`() {
        val constraints = JvmLoadingConstraintSet()
        val app = JvmClassLoaderIdentity.UserDefined(id = 1, displayName = "app")
        val plugin = JvmClassLoaderIdentity.UserDefined(id = 2, displayName = "plugin")
        val bootstrapClass = JvmLoadedClassKey("pkg/Foo", JvmClassLoaderIdentity.Bootstrap)
        val pluginClass = JvmLoadedClassKey("pkg/Foo", plugin)

        constraints.addConstraint("pkg/Foo", app, plugin)
        constraints.recordResolution("pkg/Foo", app, bootstrapClass)

        val failure = assertFailsWith<JvmLoadingConstraintViolationException> {
            constraints.recordResolution("pkg/Foo", plugin, pluginClass)
        }

        assertEquals("java/lang/LinkageError", failure.guestThrowableClassName)
        assertEquals("pkg/Foo", failure.internalName)
        assertEquals(bootstrapClass, failure.expectedClass)
        assertEquals(pluginClass, failure.actualClass)
    }

    @Test
    fun `adding a constraint detects incompatible existing resolutions`() {
        val constraints = JvmLoadingConstraintSet()
        val app = JvmClassLoaderIdentity.UserDefined(id = 1, displayName = "app")
        val plugin = JvmClassLoaderIdentity.UserDefined(id = 2, displayName = "plugin")
        val bootstrapClass = JvmLoadedClassKey("pkg/Foo", JvmClassLoaderIdentity.Bootstrap)
        val pluginClass = JvmLoadedClassKey("pkg/Foo", plugin)

        constraints.recordResolution("pkg/Foo", app, bootstrapClass)
        constraints.recordResolution("pkg/Foo", plugin, pluginClass)

        val failure = assertFailsWith<JvmLoadingConstraintViolationException> {
            constraints.addConstraint("pkg/Foo", app, plugin)
        }

        assertEquals("java/lang/LinkageError", failure.guestThrowableClassName)
        assertEquals(bootstrapClass, failure.expectedClass)
        assertEquals(pluginClass, failure.actualClass)
    }

    @Test
    fun `resolution records must match the constrained class name`() {
        val constraints = JvmLoadingConstraintSet()

        assertFailsWith<IllegalArgumentException> {
            constraints.recordResolution(
                internalName = "pkg/Foo",
                initiatingLoader = JvmClassLoaderIdentity.Bootstrap,
                resolvedClass = JvmLoadedClassKey("pkg/Bar", JvmClassLoaderIdentity.Bootstrap),
            )
        }
    }
}
