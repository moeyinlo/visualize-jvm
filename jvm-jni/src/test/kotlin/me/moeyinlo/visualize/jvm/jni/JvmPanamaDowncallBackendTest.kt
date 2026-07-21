package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmBooleanValue
import me.moeyinlo.visualize.jvm.runtime.JvmByteValue
import me.moeyinlo.visualize.jvm.runtime.JvmCharValue
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmShortValue
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class JvmPanamaDowncallBackendTest {
    @Test
    fun `Panama backend resolves descriptor exports through injected symbol lookup`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = listOf(
                JvmNativeMethodExportDescriptor(
                    ownerClassName = "pkg/NativeApi",
                    methodName = "call",
                    methodDescriptor = "(I)J",
                    isStatic = true,
                    symbolName = "Java_pkg_NativeApi_call",
                ),
            ),
        )
        val backend = JvmPanamaDowncallBackend(
            symbolLookup = JvmNativeSymbolLookup { path, symbolName ->
                if (path == library.path && symbolName == "Java_pkg_NativeApi_call") {
                    JvmNativeSymbolAddress(symbolName, 0x1234L)
                } else {
                    null
                }
            },
        )

        val target = backend.resolveExport(library, library.exports.single())

        assertEquals(
            JvmNativeDowncallTarget(
                library = library,
                guestMethod = library.exports.single().guestMethod,
                symbolName = "Java_pkg_NativeApi_call",
                address = 0x1234L,
            ),
            target,
        )
    }

    @Test
    fun `Panama backend reports unresolved native symbols`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = emptyList(),
        )
        val backend = JvmPanamaDowncallBackend(symbolLookup = JvmNativeSymbolLookup { _, _ -> null })

        assertFailsWith<JvmNativeSymbolResolutionException> {
            backend.resolveSymbol(library, "Java_pkg_NativeApi_missing")
        }
    }

    @Test
    fun `Panama backend binds optional JNI_OnLoad symbols`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = emptyList(),
        )
        val backend = JvmPanamaDowncallBackend(
            symbolLookup = JvmNativeSymbolLookup { path, symbolName ->
                if (path == library.path && symbolName == "JNI_OnLoad") {
                    JvmNativeSymbolAddress(symbolName, 0x4567L)
                } else {
                    null
                }
            },
        )
        val missingOnLoadBackend = JvmPanamaDowncallBackend(symbolLookup = JvmNativeSymbolLookup { _, _ -> null })

        assertEquals(
            JvmNativeDowncallTarget(
                library = library,
                guestMethod = null,
                symbolName = "JNI_OnLoad",
                address = 0x4567L,
            ),
            backend.bindOnLoad(library),
        )
        assertEquals(null, missingOnLoadBackend.bindOnLoad(library))
    }

    @Test
    fun `Panama backend binds Java native exports by guest signature`() {
        val staticExport = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "(I)J",
            isStatic = true,
            symbolName = "Java_pkg_NativeApi_call__I",
        )
        val instanceExport = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "poke",
            methodDescriptor = "()V",
            isStatic = false,
            symbolName = "Java_pkg_NativeApi_poke",
        )
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = listOf(staticExport, instanceExport),
        )
        val backend = JvmPanamaDowncallBackend(
            symbolLookup = JvmNativeSymbolLookup { path, symbolName ->
                when {
                    path == library.path && symbolName == staticExport.symbolName ->
                        JvmNativeSymbolAddress(symbolName, 0x1111L)
                    path == library.path && symbolName == instanceExport.symbolName ->
                        JvmNativeSymbolAddress(symbolName, 0x2222L)
                    else -> null
                }
            },
        )

        val targets = backend.bindExports(library)

        assertEquals(
            mapOf(
                staticExport.guestMethod to JvmNativeDowncallTarget(
                    library = library,
                    guestMethod = staticExport.guestMethod,
                    symbolName = staticExport.symbolName,
                    address = 0x1111L,
                ),
                instanceExport.guestMethod to JvmNativeDowncallTarget(
                    library = library,
                    guestMethod = instanceExport.guestMethod,
                    symbolName = instanceExport.symbolName,
                    address = 0x2222L,
                ),
            ),
            targets,
        )
    }

    @Test
    fun `Panama backend prepares native export invocations with simulated JNIEnv first`() {
        val export = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "(I)J",
            isStatic = true,
            symbolName = "Java_pkg_NativeApi_call__I",
        )
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = listOf(export),
        )
        val target = JvmNativeDowncallTarget(
            library = library,
            guestMethod = export.guestMethod,
            symbolName = export.symbolName,
            address = 0x3333L,
        )
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            heap = JvmHeap(),
            staticFields = JvmStaticFields(),
        )

        val invocation = target.prepareInvocation(environment, listOf(JvmIntValue(7)))

        assertEquals(target, invocation.target)
        assertSame(environment, (invocation.arguments[0] as JvmNativeDowncallArgument.SimulatedJniEnv).environment)
        assertEquals(JvmNativeDowncallArgument.IntPrimitive(7), invocation.arguments[1])
    }

    @Test
    fun `Panama backend marshals primitive guest values into JNI primitive arguments`() {
        val export = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "primitives",
            methodDescriptor = "(ZBCSIJFD)V",
            isStatic = true,
            symbolName = "Java_pkg_NativeApi_primitives__ZBCSIJFD",
        )
        val target = JvmNativeDowncallTarget(
            library = JvmNativeLibraryDescriptor(
                logicalName = "native-api",
                path = Path.of("native-api.dll"),
                exports = listOf(export),
            ),
            guestMethod = export.guestMethod,
            symbolName = export.symbolName,
            address = 0x4444L,
        )
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            heap = JvmHeap(),
            staticFields = JvmStaticFields(),
        )

        val invocation = target.prepareInvocation(
            environment = environment,
            guestArguments = listOf(
                JvmBooleanValue(true),
                JvmByteValue(-2),
                JvmCharValue('K'.code),
                JvmShortValue(123),
                JvmIntValue(456),
                JvmLongValue(789L),
                JvmFloatValue(1.5f),
                JvmDoubleValue(2.5),
            ),
        )

        assertEquals(
            listOf(
                JvmNativeDowncallArgument.SimulatedJniEnv(environment),
                JvmNativeDowncallArgument.BooleanPrimitive(true),
                JvmNativeDowncallArgument.BytePrimitive(-2),
                JvmNativeDowncallArgument.CharPrimitive('K'.code),
                JvmNativeDowncallArgument.ShortPrimitive(123),
                JvmNativeDowncallArgument.IntPrimitive(456),
                JvmNativeDowncallArgument.LongPrimitive(789L),
                JvmNativeDowncallArgument.FloatPrimitive(1.5f),
                JvmNativeDowncallArgument.DoublePrimitive(2.5),
            ),
            invocation.arguments,
        )
    }

    @Test
    fun `Panama backend marshals object references into JNI handles`() {
        val export = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "objects",
            methodDescriptor = "(Ljava/lang/Object;Ljava/lang/Object;)V",
            isStatic = true,
            symbolName = "Java_pkg_NativeApi_objects__Ljava_lang_Object_2Ljava_lang_Object_2",
        )
        val target = JvmNativeDowncallTarget(
            library = JvmNativeLibraryDescriptor(
                logicalName = "native-api",
                path = Path.of("native-api.dll"),
                exports = listOf(export),
            ),
            guestMethod = export.guestMethod,
            symbolName = export.symbolName,
            address = 0x5555L,
        )
        val heap = JvmHeap()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            heap = heap,
            staticFields = JvmStaticFields(),
        )
        val objectReference = heap.allocateObject("java/lang/Object")

        val invocation = target.prepareInvocation(
            environment = environment,
            guestArguments = listOf(objectReference, JvmNullValue),
        )

        val objectHandle = invocation.arguments[1] as JvmNativeDowncallArgument.ObjectHandle
        assertEquals(objectReference, environment.handles.resolveObject(objectHandle.handle!!))
        assertEquals(JvmNativeDowncallArgument.ObjectHandle(null), invocation.arguments[2])
    }
}
