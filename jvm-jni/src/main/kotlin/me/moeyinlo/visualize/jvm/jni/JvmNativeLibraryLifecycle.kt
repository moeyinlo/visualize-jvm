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
        val binding = backend.bindLibrary(library)
        val onLoadVersion = binding.onLoadTarget
            ?.prepareOnLoadInvocation(javaVm)
            ?.let { invocation -> invokeDowncall.invoke(invocation).toOnLoadVersion() }
        return registry.markLoaded(binding, onLoadVersion)
    }
}
