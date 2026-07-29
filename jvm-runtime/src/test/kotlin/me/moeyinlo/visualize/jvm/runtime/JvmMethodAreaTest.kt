package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JvmMethodAreaTest {
    @Test
    fun `method area defines and looks up class metadata by internal name`() {
        val methodArea = JvmMethodArea()
        val entry = JvmMethodAreaEntry(
            definition = JvmClassDefinition(
                internalName = "Example",
                superclassName = "java/lang/Object",
                fields = listOf(
                    JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = true),
                ),
                methods = listOf(
                    JvmMethodDefinition(name = "run", descriptor = "()V", isStatic = false),
                ),
            ),
        )

        methodArea.defineClass(entry)

        assertEquals(1, methodArea.classCount)
        assertTrue(methodArea.hasClass("Example"))
        assertSame(entry, methodArea.getClass("Example"))
        assertEquals(listOf(entry), methodArea.toList())
    }

    @Test
    fun `method area rejects duplicate class definitions`() {
        val methodArea = JvmMethodArea()
        methodArea.defineClass(JvmMethodAreaEntry(JvmClassDefinition(internalName = "Example")))

        val exception = assertFailsWith<JvmMethodAreaDefinitionException> {
            methodArea.defineClass(JvmMethodAreaEntry(JvmClassDefinition(internalName = "Example")))
        }

        assertEquals("Class Example is already defined in the method area", exception.message)
    }

    @Test
    fun `method area distinguishes classes by defining loader key`() {
        val methodArea = JvmMethodArea()
        val bootstrapKey = JvmLoadedClassKey(
            internalName = "pkg/Example",
            definingLoader = JvmClassLoaderIdentity.Bootstrap,
        )
        val pluginKey = JvmLoadedClassKey(
            internalName = "pkg/Example",
            definingLoader = JvmClassLoaderIdentity.UserDefined(id = 1, displayName = "plugin"),
        )
        val bootstrapEntry = JvmMethodAreaEntry(
            definition = JvmClassDefinition(internalName = "pkg/Example", superclassName = "java/lang/Object"),
            loadedClassKey = bootstrapKey,
        )
        val pluginEntry = JvmMethodAreaEntry(
            definition = JvmClassDefinition(internalName = "pkg/Example", superclassName = "plugin/Base"),
            loadedClassKey = pluginKey,
        )

        methodArea.defineClass(bootstrapEntry)
        methodArea.defineClass(pluginEntry)

        assertEquals(2, methodArea.classCount)
        assertTrue(methodArea.hasClass(bootstrapKey))
        assertTrue(methodArea.hasClass(pluginKey))
        assertSame(bootstrapEntry, methodArea.getClass(bootstrapKey))
        assertSame(pluginEntry, methodArea.getClass(pluginKey))
        assertSame(bootstrapEntry, methodArea.getClass("pkg/Example"))
    }

    @Test
    fun `method area rejects blank class names`() {
        val methodArea = JvmMethodArea()

        val defineException = assertFailsWith<IllegalArgumentException> {
            methodArea.defineClass(JvmMethodAreaEntry(JvmClassDefinition(internalName = "")))
        }
        val lookupException = assertFailsWith<IllegalArgumentException> {
            methodArea.getClass("")
        }

        assertEquals("class internal name must not be blank", defineException.message)
        assertEquals("class internal name must not be blank", lookupException.message)
    }

    @Test
    fun `method area reports missing classes`() {
        val methodArea = JvmMethodArea()

        val exception = assertFailsWith<JvmMethodAreaAccessException> {
            methodArea.getClass("Missing")
        }

        assertFalse(methodArea.hasClass("Missing"))
        assertEquals("Class Missing is not defined in the method area", exception.message)
    }

    @Test
    fun `method area lists loaded superclass definitions from root to direct parent`() {
        val methodArea = JvmMethodArea()
        val loader = JvmClassLoaderIdentity.UserDefined(id = 3, displayName = "app")
        val root = JvmClassDefinition(internalName = "Root")
        val parent = JvmClassDefinition(internalName = "Parent", superclassName = "Root")
        val child = JvmClassDefinition(internalName = "Child", superclassName = "Parent")
        listOf(root, parent, child).forEach { definition ->
            methodArea.defineClass(
                JvmMethodAreaEntry(
                    definition = definition,
                    loadedClassKey = JvmLoadedClassKey(definition.internalName, loader),
                ),
            )
        }

        val definitions = methodArea.superclassDefinitionsFor(JvmLoadedClassKey("Child", loader))

        assertEquals(listOf(root, parent), definitions)
    }

    @Test
    fun `method area reports missing loaded superclass definitions without loading them`() {
        val methodArea = JvmMethodArea()
        val loader = JvmClassLoaderIdentity.UserDefined(id = 3, displayName = "app")
        methodArea.defineClass(
            JvmMethodAreaEntry(
                definition = JvmClassDefinition(internalName = "Child", superclassName = "Parent"),
                loadedClassKey = JvmLoadedClassKey("Child", loader),
            ),
        )

        val failure = assertFailsWith<JvmMethodAreaAccessException> {
            methodArea.superclassDefinitionsFor(JvmLoadedClassKey("Child", loader))
        }

        assertEquals("Superclass Parent @ app#3 of Child @ app#3 is not defined in the method area", failure.message)
        assertFalse(methodArea.hasClass(JvmLoadedClassKey("Parent", loader)))
    }

    @Test
    fun `method area returns no superclass definitions for java lang Object`() {
        val methodArea = JvmMethodArea()
        methodArea.defineClass(
            JvmMethodAreaEntry(
                definition = JvmClassDefinition(internalName = "java/lang/Object"),
                loadedClassKey = JvmLoadedClassKey("java/lang/Object", JvmClassLoaderIdentity.Bootstrap),
            ),
        )

        val definitions = methodArea.superclassDefinitionsFor(
            JvmLoadedClassKey("java/lang/Object", JvmClassLoaderIdentity.Bootstrap),
        )

        assertEquals(emptyList(), definitions)
    }

    @Test
    fun `method area exposes a class hierarchy view over defined classes`() {
        val methodArea = JvmMethodArea()
        methodArea.defineClass(
            JvmMethodAreaEntry(
                JvmClassDefinition(
                    internalName = "Parent",
                    methods = listOf(
                        JvmMethodDefinition(name = "value", descriptor = "()I", isStatic = false),
                    ),
                ),
            ),
        )
        methodArea.defineClass(
            JvmMethodAreaEntry(
                JvmClassDefinition(
                    internalName = "Child",
                    superclassName = "Parent",
                ),
            ),
        )

        val resolved = methodArea.classHierarchy().resolveVirtualMethod("Child", "value", "()I")

        assertEquals("Parent", resolved.ownerClassName)
        assertEquals("value", resolved.name)
        assertEquals("()I", resolved.descriptor)
    }
    @Test
    fun `method area runtime nestmates require the same defining loader runtime package`() {
        val methodArea = JvmMethodArea()
        val appLoader = JvmClassLoaderIdentity.UserDefined(id = 21, displayName = "app")
        val pluginLoader = JvmClassLoaderIdentity.UserDefined(id = 22, displayName = "plugin")
        val hostKey = JvmLoadedClassKey("pkg/Host", appLoader)
        val appMemberKey = JvmLoadedClassKey("pkg/Host\$Member", appLoader)
        val pluginMemberKey = JvmLoadedClassKey("pkg/Host\$Member", pluginLoader)
        methodArea.defineClass(
            JvmMethodAreaEntry(
                definition = JvmClassDefinition(
                    internalName = "pkg/Host",
                    nestMemberInternalNames = listOf("pkg/Host\$Member"),
                ),
                loadedClassKey = hostKey,
            ),
        )
        methodArea.defineClass(
            JvmMethodAreaEntry(
                definition = JvmClassDefinition(
                    internalName = "pkg/Host\$Member",
                    nestHostInternalName = "pkg/Host",
                ),
                loadedClassKey = appMemberKey,
            ),
        )
        methodArea.defineClass(
            JvmMethodAreaEntry(
                definition = JvmClassDefinition(
                    internalName = "pkg/Host\$Member",
                    nestHostInternalName = "pkg/Host",
                ),
                loadedClassKey = pluginMemberKey,
            ),
        )

        assertTrue(methodArea.areRuntimeNestmates(hostKey, appMemberKey))
        assertFalse(methodArea.areRuntimeNestmates(hostKey, pluginMemberKey))
    }
    @Test
    fun `method area runtime nestmate diagnostics report missing nest hosts`() {
        val methodArea = JvmMethodArea()
        val loader = JvmClassLoaderIdentity.UserDefined(id = 23, displayName = "app")
        val firstKey = JvmLoadedClassKey("pkg/First", loader)
        val secondKey = JvmLoadedClassKey("pkg/Second", loader)
        methodArea.defineClass(
            JvmMethodAreaEntry(
                definition = JvmClassDefinition(
                    internalName = "pkg/First",
                    nestHostInternalName = "pkg/MissingHost",
                ),
                loadedClassKey = firstKey,
            ),
        )
        methodArea.defineClass(
            JvmMethodAreaEntry(
                definition = JvmClassDefinition(
                    internalName = "pkg/Second",
                    nestHostInternalName = "pkg/MissingHost",
                ),
                loadedClassKey = secondKey,
            ),
        )

        val check = methodArea.checkRuntimeNestmates(firstKey, secondKey)

        assertFalse(check.areNestmates)
        assertEquals(
            JvmRuntimeNestmateFailure.MissingHost(
                memberKey = firstKey,
                hostKey = JvmLoadedClassKey("pkg/MissingHost", loader),
            ),
            check.failure,
        )
    }
    @Test
    fun `method area entry exposes runtime package and module metadata`() {
        val loader = JvmClassLoaderIdentity.UserDefined(id = 11, displayName = "app-loader")
        val key = JvmLoadedClassKey("app/internal/Example", loader)
        val entry = JvmMethodAreaEntry(
            definition = JvmClassDefinition(internalName = "app/internal/Example"),
            loadedClassKey = key,
            runtimeModuleName = "app.module",
        )
        val methodArea = JvmMethodArea()

        methodArea.defineClass(entry)

        val loaded = methodArea.getClass(key)
        assertEquals("app.module", loaded?.runtimeModuleName)
        assertEquals(JvmRuntimePackageKey("app/internal", loader), loaded?.runtimePackageKey)
    }

    @Test
    fun `method area entry rejects blank runtime module names`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            JvmMethodAreaEntry(
                definition = JvmClassDefinition(internalName = "Example"),
                runtimeModuleName = " ",
            )
        }

        assertEquals("runtime module name must not be blank", failure.message)
    }
    @Test
    fun `method area lists classes in a runtime package`() {
        val methodArea = JvmMethodArea()
        val loader = JvmClassLoaderIdentity.UserDefined(id = 7, displayName = "app")
        val packageKey = JvmRuntimePackageKey("pkg", loader)
        val first = JvmMethodAreaEntry(
            definition = JvmClassDefinition(internalName = "pkg/First"),
            loadedClassKey = JvmLoadedClassKey("pkg/First", loader),
        )
        val second = JvmMethodAreaEntry(
            definition = JvmClassDefinition(internalName = "pkg/Second"),
            loadedClassKey = JvmLoadedClassKey("pkg/Second", loader),
        )
        val bootstrapSamePackage = JvmMethodAreaEntry(
            definition = JvmClassDefinition(internalName = "pkg/Bootstrap"),
            loadedClassKey = JvmLoadedClassKey("pkg/Bootstrap", JvmClassLoaderIdentity.Bootstrap),
        )
        val otherPackage = JvmMethodAreaEntry(
            definition = JvmClassDefinition(internalName = "other/Other"),
            loadedClassKey = JvmLoadedClassKey("other/Other", loader),
        )
        val arrayClass = JvmMethodAreaEntry(
            definition = JvmClassDefinition(internalName = "[Lpkg/First;"),
            loadedClassKey = JvmLoadedClassKey("[Lpkg/First;", loader),
        )

        listOf(first, second, bootstrapSamePackage, otherPackage, arrayClass).forEach(methodArea::defineClass)

        assertEquals(listOf(first, second), methodArea.classesInRuntimePackage(packageKey))
    }
    @Test
    fun `method area rejects different modules in one runtime package`() {
        val methodArea = JvmMethodArea()
        val loader = JvmClassLoaderIdentity.UserDefined(id = 7, displayName = "app")
        methodArea.defineClass(
            JvmMethodAreaEntry(
                definition = JvmClassDefinition(internalName = "pkg/First"),
                loadedClassKey = JvmLoadedClassKey("pkg/First", loader),
                runtimeModuleName = "first.module",
            ),
        )

        val failure = assertFailsWith<JvmMethodAreaDefinitionException> {
            methodArea.defineClass(
                JvmMethodAreaEntry(
                    definition = JvmClassDefinition(internalName = "pkg/Second"),
                    loadedClassKey = JvmLoadedClassKey("pkg/Second", loader),
                    runtimeModuleName = "second.module",
                ),
            )
        }

        assertEquals(
            "Runtime package pkg @ app#7 is already associated with module first.module, " +
                "cannot define pkg/Second in module second.module",
            failure.message,
        )
        assertEquals(1, methodArea.classCount)
        assertFalse(methodArea.hasClass(JvmLoadedClassKey("pkg/Second", loader)))
    }
}
