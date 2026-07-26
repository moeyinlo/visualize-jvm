package me.moeyinlo.visualize.jvm.jni

fun interface JvmNativeDowncallInvoker {
    fun invoke(invocation: JvmNativeDowncallInvocation): JvmNativeDowncallReturn
}

class JvmNativeLibraryLifecycle(
    private val backend: JvmPanamaDowncallBackend,
    private val registry: JvmNativeLibraryRegistry,
    private val invokeDowncall: JvmNativeDowncallInvoker,
) {
    fun load(
        library: JvmNativeLibraryDescriptor,
        javaVm: JvmSimulatedJavaVm,
    ): JvmLoadedNativeLibrary {
        registry.loadedLibrary(library.logicalName)?.let {
            throw JvmNativeLibraryLoadException("native library ${library.logicalName} is already loaded")
        }
        val binding = backend.bindLibrary(library)
        val onLoadVersion = binding.onLoadTarget
            ?.prepareOnLoadInvocation(javaVm)
            ?.let { invocation ->
                javaVm.withNativeLibraryLifecycleLocalFrame {
                    invokeDowncall.invoke(invocation).toOnLoadVersion()
                }
            }
        return registry.markLoaded(binding, onLoadVersion)
    }

    fun unload(
        logicalName: String,
        javaVm: JvmSimulatedJavaVm,
    ): JvmLoadedNativeLibrary {
        val request = registry.unloadOrThrow(logicalName, javaVm)
        request.onUnloadInvocation
            ?.let { invocation ->
                javaVm.withNativeLibraryLifecycleLocalFrame {
                    invokeDowncall.invoke(invocation)
                }
            }
            ?.requireOnUnloadVoid()
        return request.loadedLibrary
    }
}

private inline fun <T> JvmSimulatedJavaVm.withNativeLibraryLifecycleLocalFrame(action: () -> T): T {
    environment.pushLocalFrame(NativeLibraryLifecycleLocalCapacity)
    try {
        return action()
    } finally {
        environment.popLocalFrame(null)
    }
}

private const val NativeLibraryLifecycleLocalCapacity: Int = 16
