package me.moeyinlo.visualize.jvm.jni

import java.nio.file.Path

data class JvmNativeSymbolAddress(
    val symbolName: String,
    val address: Long,
) {
    init {
        require(symbolName.isNotBlank()) { "native symbol name must not be blank" }
        require(address != 0L) { "native symbol address must be non-zero" }
    }
}

fun interface JvmNativeSymbolLookup {
    fun find(libraryPath: Path, symbolName: String): JvmNativeSymbolAddress?
}

data class JvmNativeDowncallTarget(
    val library: JvmNativeLibraryDescriptor,
    val guestMethod: JvmNativeGuestMethodSignature?,
    val symbolName: String,
    val address: Long,
) {
    init {
        require(symbolName.isNotBlank()) { "native downcall symbol name must not be blank" }
        require(address != 0L) { "native downcall address must be non-zero" }
    }
}

class JvmPanamaDowncallBackend(
    private val symbolLookup: JvmNativeSymbolLookup,
) {
    fun resolveExport(
        library: JvmNativeLibraryDescriptor,
        export: JvmNativeMethodExportDescriptor,
    ): JvmNativeDowncallTarget {
        val address = resolveSymbol(library, export.symbolName)
        return JvmNativeDowncallTarget(
            library = library,
            guestMethod = export.guestMethod,
            symbolName = address.symbolName,
            address = address.address,
        )
    }

    fun bindOnLoad(library: JvmNativeLibraryDescriptor): JvmNativeDowncallTarget? {
        val address = symbolLookup.find(library.path, library.onLoadSymbol) ?: return null
        return JvmNativeDowncallTarget(
            library = library,
            guestMethod = null,
            symbolName = address.symbolName,
            address = address.address,
        )
    }

    fun bindExports(library: JvmNativeLibraryDescriptor): Map<JvmNativeGuestMethodSignature, JvmNativeDowncallTarget> =
        library.exports.associate { export ->
            export.guestMethod to resolveExport(library, export)
        }

    fun resolveSymbol(
        library: JvmNativeLibraryDescriptor,
        symbolName: String,
    ): JvmNativeSymbolAddress {
        require(symbolName.isNotBlank()) { "native symbol name must not be blank" }
        return symbolLookup.find(library.path, symbolName)
            ?: throw JvmNativeSymbolResolutionException(
                "Native library ${library.logicalName} does not export $symbolName at ${library.path}",
            )
    }
}

class JvmNativeSymbolResolutionException(message: String) : UnsatisfiedLinkError(message)
