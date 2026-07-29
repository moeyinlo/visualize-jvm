package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class JvmArrayClassFactoryTest {
    @Test
    fun `creates primitive array class metadata with JVMS superclass and interfaces`() {
        val methodArea = JvmMethodArea()
        val factory = JvmArrayClassFactory(methodArea)

        val arrayClass = factory.createPrimitiveArrayClass(JvmPrimitiveArrayComponent.Int)

        assertEquals("[I", arrayClass.definition.internalName)
        assertEquals("java/lang/Object", arrayClass.definition.superclassName)
        assertEquals(
            listOf("java/lang/Cloneable", "java/io/Serializable"),
            arrayClass.definition.interfaceNames,
        )
        assertEquals(
            JvmLoadedClassKey("[I", JvmClassLoaderIdentity.Bootstrap),
            arrayClass.loadedClassKey,
        )
        assertEquals(setOf(JvmClassLoaderIdentity.Bootstrap), arrayClass.initiatingLoaders)
        assertEquals(
            JvmArrayComponent.Primitive(JvmPrimitiveArrayComponent.Int),
            factory.metadataFor("[I")?.component,
        )
        assertSame(arrayClass, methodArea.getClass("[I"))
    }

    @Test
    fun `reuses already created primitive array class metadata`() {
        val methodArea = JvmMethodArea()
        val factory = JvmArrayClassFactory(methodArea)
        val firstArrayClass = factory.createPrimitiveArrayClass(JvmPrimitiveArrayComponent.Int)

        val secondArrayClass = factory.createPrimitiveArrayClass(JvmPrimitiveArrayComponent.Int)

        assertSame(firstArrayClass, secondArrayClass)
        assertSame(firstArrayClass, methodArea.getClass("[I"))
        assertEquals(1, methodArea.classCount)
        assertEquals(
            JvmArrayComponent.Primitive(JvmPrimitiveArrayComponent.Int),
            factory.metadataFor("[I")?.component,
        )
    }

    @Test
    fun `creates reference array class metadata with component defining loader`() {
        val methodArea = JvmMethodArea()
        val factory = JvmArrayClassFactory(methodArea)
        val componentLoader = JvmClassLoaderIdentity.UserDefined(id = 7, displayName = "app")
        val loadedClassKey = JvmLoadedClassKey("[Ljava/lang/String;", componentLoader)

        val arrayClass = factory.createReferenceArrayClass(
            componentInternalName = "java/lang/String",
            componentDefiningLoader = componentLoader,
        )

        assertEquals("[Ljava/lang/String;", arrayClass.definition.internalName)
        assertEquals("java/lang/Object", arrayClass.definition.superclassName)
        assertEquals(
            listOf("java/lang/Cloneable", "java/io/Serializable"),
            arrayClass.definition.interfaceNames,
        )
        assertEquals(loadedClassKey, arrayClass.loadedClassKey)
        assertEquals(setOf(componentLoader), arrayClass.initiatingLoaders)
        assertEquals(
            JvmArrayComponent.Reference("java/lang/String", componentLoader),
            factory.metadataFor(loadedClassKey)?.component,
        )
        assertSame(arrayClass, methodArea.getClass("[Ljava/lang/String;", componentLoader))
    }

    @Test
    fun `distinguishes reference array metadata by component defining loader`() {
        val methodArea = JvmMethodArea()
        val factory = JvmArrayClassFactory(methodArea)
        val appLoader = JvmClassLoaderIdentity.UserDefined(id = 7, displayName = "app")
        val pluginLoader = JvmClassLoaderIdentity.UserDefined(id = 8, displayName = "plugin")
        val appKey = JvmLoadedClassKey("[Lpkg/Type;", appLoader)
        val pluginKey = JvmLoadedClassKey("[Lpkg/Type;", pluginLoader)

        val appArrayClass = factory.createReferenceArrayClass("pkg/Type", appLoader)
        val pluginArrayClass = factory.createReferenceArrayClass("pkg/Type", pluginLoader)

        assertEquals(appKey, appArrayClass.loadedClassKey)
        assertEquals(pluginKey, pluginArrayClass.loadedClassKey)
        assertSame(appArrayClass, methodArea.getClass(appKey))
        assertSame(pluginArrayClass, methodArea.getClass(pluginKey))
        assertEquals(2, methodArea.classCount)
        assertEquals(JvmArrayComponent.Reference("pkg/Type", appLoader), factory.metadataFor(appKey)?.component)
        assertEquals(JvmArrayComponent.Reference("pkg/Type", pluginLoader), factory.metadataFor(pluginKey)?.component)
    }

    @Test
    fun `creates multidimensional array class metadata from an array component class`() {
        val methodArea = JvmMethodArea()
        val factory = JvmArrayClassFactory(methodArea)
        val intArrayClass = factory.createPrimitiveArrayClass(JvmPrimitiveArrayComponent.Int)
        val intArrayKey = JvmLoadedClassKey("[I", JvmClassLoaderIdentity.Bootstrap)
        val twoDimensionalKey = JvmLoadedClassKey("[[I", JvmClassLoaderIdentity.Bootstrap)

        val twoDimensionalArrayClass = factory.createArrayClassWithArrayComponent(intArrayClass)

        assertEquals("[[I", twoDimensionalArrayClass.definition.internalName)
        assertEquals("java/lang/Object", twoDimensionalArrayClass.definition.superclassName)
        assertEquals(
            listOf("java/lang/Cloneable", "java/io/Serializable"),
            twoDimensionalArrayClass.definition.interfaceNames,
        )
        assertEquals(twoDimensionalKey, twoDimensionalArrayClass.loadedClassKey)
        assertEquals(setOf(JvmClassLoaderIdentity.Bootstrap), twoDimensionalArrayClass.initiatingLoaders)
        assertEquals(JvmArrayComponent.Array(intArrayKey), factory.metadataFor(twoDimensionalKey)?.component)
        assertSame(twoDimensionalArrayClass, methodArea.getClass(twoDimensionalKey))
        assertEquals(2, methodArea.classCount)
    }
}
