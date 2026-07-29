package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmLoadedClassKey

data class JvmJniNativeMethodDescriptor(
    val name: String,
    val descriptor: String,
    val functionAddress: Long,
) {
    init {
        require(name.isNotBlank()) { "registered native method name must not be blank" }
        require(descriptor.isNotBlank()) { "registered native method descriptor must not be blank" }
        require(functionAddress != 0L) { "registered native method function pointer must be non-zero" }
    }
}

data class JvmJniRegisteredNativeMethod(
    val className: String,
    val name: String,
    val descriptor: String,
    val functionAddress: Long,
    val loadedClassKey: JvmLoadedClassKey? = null,
) {
    init {
        require(className.isNotBlank()) { "registered native method class name must not be blank" }
        require(name.isNotBlank()) { "registered native method name must not be blank" }
        require(descriptor.isNotBlank()) { "registered native method descriptor must not be blank" }
        require(functionAddress != 0L) { "registered native method function pointer must be non-zero" }
    }
}

class JvmJniNativeMethodRegistry {
    private val methodsByKey = mutableMapOf<JvmJniRegisteredNativeMethodKey, JvmJniRegisteredNativeMethod>()

    fun register(
        className: String,
        methods: List<JvmJniNativeMethodDescriptor>,
        loadedClassKey: JvmLoadedClassKey? = null,
    ): Int {
        require(className.isNotBlank()) { "registered native method class name must not be blank" }
        val duplicate = methods
            .groupingBy { method -> method.key(className, loadedClassKey) }
            .eachCount()
            .entries
            .firstOrNull { (_, count) -> count > 1 }
        require(duplicate == null) {
            "duplicate registered native method ${duplicate!!.key.name}:${duplicate.key.descriptor} for $className"
        }

        methods.forEach { descriptor ->
            val method = JvmJniRegisteredNativeMethod(
                className = className,
                name = descriptor.name,
                descriptor = descriptor.descriptor,
                functionAddress = descriptor.functionAddress,
                loadedClassKey = loadedClassKey,
            )
            methodsByKey[method.key] = method
        }
        return 0
    }

    fun unregister(
        className: String,
        loadedClassKey: JvmLoadedClassKey? = null,
    ): Int {
        require(className.isNotBlank()) { "registered native method class name must not be blank" }
        methodsByKey.keys
            .filter { key -> key.className == className && key.loadedClassKey == loadedClassKey }
            .forEach(methodsByKey::remove)
        return 0
    }

    fun resolve(
        className: String,
        name: String,
        descriptor: String,
        loadedClassKey: JvmLoadedClassKey? = null,
    ): JvmJniRegisteredNativeMethod? =
        methodsByKey[JvmJniRegisteredNativeMethodKey(className, loadedClassKey, name, descriptor)]

    fun resolveDowncallTarget(
        library: JvmNativeLibraryDescriptor,
        className: String,
        name: String,
        descriptor: String,
        isStatic: Boolean,
        loadedClassKey: JvmLoadedClassKey? = null,
    ): JvmNativeDowncallTarget? =
        resolve(
            className = className,
            name = name,
            descriptor = descriptor,
            loadedClassKey = loadedClassKey,
        )?.toDowncallTarget(library = library, isStatic = isStatic)

    fun entriesForClass(className: String): List<JvmJniRegisteredNativeMethod> =
        methodsByKey.values
            .filter { method -> method.className == className }
            .sortedWith(compareBy<JvmJniRegisteredNativeMethod> { method -> method.name }.thenBy { method -> method.descriptor })

    private fun JvmJniNativeMethodDescriptor.key(
        className: String,
        loadedClassKey: JvmLoadedClassKey?,
    ): JvmJniRegisteredNativeMethodKey =
        JvmJniRegisteredNativeMethodKey(
            className = className,
            loadedClassKey = loadedClassKey,
            name = name,
            descriptor = descriptor,
        )

    private val JvmJniRegisteredNativeMethod.key: JvmJniRegisteredNativeMethodKey
        get() = JvmJniRegisteredNativeMethodKey(
            className = className,
            loadedClassKey = loadedClassKey,
            name = name,
            descriptor = descriptor,
        )

    private fun JvmJniRegisteredNativeMethod.toDowncallTarget(
        library: JvmNativeLibraryDescriptor,
        isStatic: Boolean,
    ): JvmNativeDowncallTarget =
        JvmNativeDowncallTarget(
            library = library,
            guestMethod = JvmNativeGuestMethodSignature(
                ownerClassName = className,
                methodName = name,
                methodDescriptor = descriptor,
                isStatic = isStatic,
            ),
            symbolName = "RegisterNatives:$className.$name:$descriptor",
            address = functionAddress,
        )
}

private data class JvmJniRegisteredNativeMethodKey(
    val className: String,
    val loadedClassKey: JvmLoadedClassKey?,
    val name: String,
    val descriptor: String,
)
