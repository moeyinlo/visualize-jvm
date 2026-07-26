package me.moeyinlo.visualize.jvm.jni

import java.nio.file.Path

data class JvmNativeMethodExportDescriptor(
    val ownerClassName: String,
    val methodName: String,
    val methodDescriptor: String,
    val isStatic: Boolean,
    val symbolName: String,
) {
    init {
        require(ownerClassName.isNotBlank()) { "native export owner class name must not be blank" }
        require(methodName.isNotBlank()) { "native export method name must not be blank" }
        require(methodDescriptor.isNotBlank()) { "native export method descriptor must not be blank" }
        require(symbolName.isNotBlank()) { "native export symbol name must not be blank" }
    }

    val guestMethod: JvmNativeGuestMethodSignature = JvmNativeGuestMethodSignature(
        ownerClassName = ownerClassName,
        methodName = methodName,
        methodDescriptor = methodDescriptor,
        isStatic = isStatic,
    )
}

data class JvmNativeGuestMethodSignature(
    val ownerClassName: String,
    val methodName: String,
    val methodDescriptor: String,
    val isStatic: Boolean,
)

data class JvmNativeLibraryDescriptor(
    val logicalName: String,
    val path: Path,
    val exports: List<JvmNativeMethodExportDescriptor> = emptyList(),
    val onLoadSymbol: String = "JNI_OnLoad",
    val onUnloadSymbol: String = "JNI_OnUnload",
) {
    init {
        require(logicalName.isNotBlank()) { "native library logical name must not be blank" }
        require(path.toString().isNotBlank()) { "native library path must not be blank" }
        require(onLoadSymbol.isNotBlank()) { "native library JNI_OnLoad symbol must not be blank" }
        require(onUnloadSymbol.isNotBlank()) { "native library JNI_OnUnload symbol must not be blank" }

        val duplicate = exports
            .groupingBy { export -> export.guestMethod }
            .eachCount()
            .entries
            .firstOrNull { (_, count) -> count > 1 }
        require(duplicate == null) {
            "native library $logicalName has duplicate guest export ${duplicate!!.key}"
        }
    }

    fun exportFor(
        ownerClassName: String,
        methodName: String,
        methodDescriptor: String,
        isStatic: Boolean,
    ): JvmNativeMethodExportDescriptor? {
        val signature = JvmNativeGuestMethodSignature(
            ownerClassName = ownerClassName,
            methodName = methodName,
            methodDescriptor = methodDescriptor,
            isStatic = isStatic,
        )
        return exports.firstOrNull { export -> export.guestMethod == signature }
    }
}
