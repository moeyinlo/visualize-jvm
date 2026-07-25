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
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
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
    fun `Panama backend accepts supported JNI_OnLoad versions`() {
        assertEquals(
            0x00180000,
            JvmNativeDowncallReturn.IntPrimitive(0x00180000).toOnLoadVersion(),
        )
    }

    @Test
    fun `Panama backend rejects unsupported JNI_OnLoad returns`() {
        assertFailsWith<JvmJniOnLoadException> {
            JvmNativeDowncallReturn.IntPrimitive(0x7fff0000).toOnLoadVersion()
        }
        assertFailsWith<JvmJniOnLoadException> {
            JvmNativeDowncallReturn.Void.toOnLoadVersion()
        }
    }
    @Test
    fun `Panama backend prepares JNI_OnLoad invocation with simulated JavaVM and reserved null`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = emptyList(),
        )
        val target = JvmNativeDowncallTarget(
            library = library,
            guestMethod = null,
            symbolName = "JNI_OnLoad",
            address = 0x4567L,
        )
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            staticFields = JvmStaticFields(),
        )
        val javaVm = JvmSimulatedJavaVm(environment)

        val invocation = target.prepareOnLoadInvocation(javaVm)

        assertEquals(target, invocation.target)
        assertEquals(
            listOf(
                JvmNativeDowncallArgument.SimulatedJavaVm(javaVm),
                JvmNativeDowncallArgument.ReservedNull,
            ),
            invocation.arguments,
        )
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
    fun `Panama backend binds native libraries as OnLoad plus export targets`() {
        val export = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "()I",
            isStatic = true,
            symbolName = "Java_pkg_NativeApi_call",
        )
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = listOf(export),
        )
        val backend = JvmPanamaDowncallBackend(
            symbolLookup = JvmNativeSymbolLookup { path, symbolName ->
                when {
                    path == library.path && symbolName == "JNI_OnLoad" -> JvmNativeSymbolAddress(symbolName, 0x1111L)
                    path == library.path && symbolName == export.symbolName -> JvmNativeSymbolAddress(symbolName, 0x2222L)
                    else -> null
                }
            },
        )

        val binding = backend.bindLibrary(library)

        assertEquals(library, binding.library)
        assertEquals(
            JvmNativeDowncallTarget(library = library, guestMethod = null, symbolName = "JNI_OnLoad", address = 0x1111L),
            binding.onLoadTarget,
        )
        assertEquals(
            JvmNativeDowncallTarget(
                library = library,
                guestMethod = export.guestMethod,
                symbolName = export.symbolName,
                address = 0x2222L,
            ),
            binding.exportTargets.getValue(export.guestMethod),
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
    fun `Panama backend prepares instance native invocations with receiver after simulated JNIEnv`() {
        val export = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "(I)V",
            isStatic = false,
            symbolName = "Java_pkg_NativeApi_call__I",
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
        val heap = JvmHeap()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            heap = heap,
            staticFields = JvmStaticFields(),
        )
        val receiver = heap.allocateObject("pkg/NativeApi")

        val invocation = target.prepareInstanceInvocation(
            environment = environment,
            receiver = receiver,
            guestArguments = listOf(JvmIntValue(7)),
        )

        assertSame(environment, (invocation.arguments[0] as JvmNativeDowncallArgument.SimulatedJniEnv).environment)
        val receiverHandle = invocation.arguments[1] as JvmNativeDowncallArgument.ObjectHandle
        assertEquals(receiver, environment.handles.resolveObject(receiverHandle.handle!!))
        assertEquals(JvmNativeDowncallArgument.IntPrimitive(7), invocation.arguments[2])
    }
    @Test
    fun `Panama backend prepares static native invocations with class after simulated JNIEnv`() {
        val export = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "(I)V",
            isStatic = true,
            symbolName = "Java_pkg_NativeApi_call__I",
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
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            staticFields = JvmStaticFields(),
        )
        val classHandle = environment.handles.newClassHandle("pkg/NativeApi")

        val invocation = target.prepareStaticInvocation(
            environment = environment,
            classHandle = classHandle,
            guestArguments = listOf(JvmIntValue(7)),
        )

        assertSame(environment, (invocation.arguments[0] as JvmNativeDowncallArgument.SimulatedJniEnv).environment)
        assertEquals(JvmNativeDowncallArgument.ClassHandle(classHandle), invocation.arguments[1])
        assertEquals(JvmNativeDowncallArgument.IntPrimitive(7), invocation.arguments[2])
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

    @Test
    fun `Panama backend marshals JNI return values back into guest values`() {
        val heap = JvmHeap()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            heap = heap,
            staticFields = JvmStaticFields(),
        )
        val objectReference = heap.allocateObject("java/lang/Object")
        val objectHandle = environment.handles.newObjectHandle(objectReference)

        assertEquals(null, JvmNativeDowncallReturn.Void.toGuestValue(environment))
        assertEquals(JvmBooleanValue(true), JvmNativeDowncallReturn.BooleanPrimitive(true).toGuestValue(environment))
        assertEquals(JvmByteValue(-8), JvmNativeDowncallReturn.BytePrimitive(-8).toGuestValue(environment))
        assertEquals(JvmCharValue('R'.code), JvmNativeDowncallReturn.CharPrimitive('R'.code).toGuestValue(environment))
        assertEquals(JvmShortValue(32000), JvmNativeDowncallReturn.ShortPrimitive(32000).toGuestValue(environment))
        assertEquals(JvmIntValue(1234), JvmNativeDowncallReturn.IntPrimitive(1234).toGuestValue(environment))
        assertEquals(JvmLongValue(5678L), JvmNativeDowncallReturn.LongPrimitive(5678L).toGuestValue(environment))
        assertEquals(JvmFloatValue(3.25f), JvmNativeDowncallReturn.FloatPrimitive(3.25f).toGuestValue(environment))
        assertEquals(JvmDoubleValue(6.5), JvmNativeDowncallReturn.DoublePrimitive(6.5).toGuestValue(environment))
        assertEquals(objectReference, JvmNativeDowncallReturn.ObjectHandle(objectHandle).toGuestValue(environment))
        assertEquals(JvmNullValue, JvmNativeDowncallReturn.ObjectHandle(null).toGuestValue(environment))
    }

    @Test
    fun `Panama backend propagates native thrown guest exceptions`() {
        val heap = JvmHeap()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            heap = heap,
            staticFields = JvmStaticFields(),
        )
        val throwableReference = heap.allocateObject("java/lang/IllegalStateException")
        val throwableHandle = environment.handles.newObjectHandle(throwableReference)

        val thrown = assertFailsWith<JvmNativeGuestException> {
            JvmNativeDowncallReturn.ThrownGuestException(throwableHandle).toGuestValue(environment)
        }

        assertEquals(throwableReference, thrown.throwable)
    }

    @Test
    fun `Panama backend propagates pending JNI exceptions after native returns`() {
        val heap = JvmHeap()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            heap = heap,
            staticFields = JvmStaticFields(),
        )
        val throwableReference = heap.allocateObject("java/lang/IllegalStateException")
        val throwableHandle = environment.handles.newObjectHandle(throwableReference)
        environment.throwObject(throwableHandle)

        val thrown = assertFailsWith<JvmNativeGuestException> {
            JvmNativeDowncallReturn.Void.toGuestValue(environment)
        }

        assertEquals(throwableReference, thrown.throwable)
        assertEquals(false, environment.exceptionCheck())
    }

    @Test
    fun `Panama backend runs tiny native library with upcall fixture`(@TempDir tempDir: Path) {
        val library = compileTinyUpcallFixture(tempDir)

        Arena.ofConfined().use { arena ->
            val linker = Linker.nativeLinker()
            val symbol = SymbolLookup.libraryLookup(library, arena)
                .find("jvm_tiny_upcall_fixture")
                .orElseThrow()
            val downcall = linker.downcallHandle(
                symbol,
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS),
            )
            val upcallHandle = MethodHandles.lookup().findStatic(
                JvmPanamaDowncallBackendTest::class.java,
                "fixtureUpcall",
                MethodType.methodType(Integer.TYPE, Integer.TYPE),
            )
            val upcallStub = linker.upcallStub(
                upcallHandle,
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT),
                arena,
            )

            assertEquals(45, downcall.invokeWithArguments(2, upcallStub))
        }
    }

    private fun compileTinyUpcallFixture(tempDir: Path): Path {
        assumeTrue(commandSucceeds("clang", "--version"), "clang is required for the tiny native upcall fixture")

        val source = tempDir.resolve("tiny_upcall_fixture.c")
        val library = tempDir.resolve("tiny_upcall_fixture.dll")
        Files.writeString(
            source,
            """
            __declspec(dllexport) int jvm_tiny_upcall_fixture(int value, int (__cdecl *upcall)(int)) {
                return upcall(value) + 3;
            }
            """.trimIndent(),
        )
        val output = runCommand("clang", "-shared", "-o", library.toString(), source.toString())
        assumeTrue(output.exitCode == 0, "clang failed to build tiny native upcall fixture: ${output.text}")
        return library
    }

    private fun commandSucceeds(vararg command: String): Boolean =
        runCommand(*command).exitCode == 0

    private fun runCommand(vararg command: String): ProcessOutput {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        val text = process.inputStream.bufferedReader().use { reader -> reader.readText() }
        return ProcessOutput(process.waitFor(), text)
    }

    private data class ProcessOutput(
        val exitCode: Int,
        val text: String,
    )

    companion object {
        @JvmStatic
        fun fixtureUpcall(value: Int): Int = value + 40
    }
}
