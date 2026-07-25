package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmBooleanValue
import me.moeyinlo.visualize.jvm.runtime.JvmByteValue
import me.moeyinlo.visualize.jvm.runtime.JvmCharValue
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmShortValue
import me.moeyinlo.visualize.jvm.runtime.JvmValue
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

data class JvmNativeDowncallInvocation(
    val target: JvmNativeDowncallTarget,
    val arguments: List<JvmNativeDowncallArgument>,
)

sealed interface JvmNativeDowncallArgument {
    data class SimulatedJniEnv(val environment: JvmSimulatedJniEnvironment) : JvmNativeDowncallArgument
    data class BooleanPrimitive(val value: Boolean) : JvmNativeDowncallArgument
    data class BytePrimitive(val value: Int) : JvmNativeDowncallArgument
    data class CharPrimitive(val value: Int) : JvmNativeDowncallArgument
    data class ShortPrimitive(val value: Int) : JvmNativeDowncallArgument
    data class IntPrimitive(val value: Int) : JvmNativeDowncallArgument
    data class LongPrimitive(val value: Long) : JvmNativeDowncallArgument
    data class FloatPrimitive(val value: Float) : JvmNativeDowncallArgument
    data class DoublePrimitive(val value: Double) : JvmNativeDowncallArgument
    data class ObjectHandle(val handle: JvmJniHandleId?) : JvmNativeDowncallArgument
    data class ClassHandle(val handle: JvmJniHandleId) : JvmNativeDowncallArgument
    data class GuestValue(val value: JvmValue) : JvmNativeDowncallArgument
}

sealed interface JvmNativeDowncallReturn {
    data object Void : JvmNativeDowncallReturn
    data class BooleanPrimitive(val value: Boolean) : JvmNativeDowncallReturn
    data class BytePrimitive(val value: Int) : JvmNativeDowncallReturn
    data class CharPrimitive(val value: Int) : JvmNativeDowncallReturn
    data class ShortPrimitive(val value: Int) : JvmNativeDowncallReturn
    data class IntPrimitive(val value: Int) : JvmNativeDowncallReturn
    data class LongPrimitive(val value: Long) : JvmNativeDowncallReturn
    data class FloatPrimitive(val value: Float) : JvmNativeDowncallReturn
    data class DoublePrimitive(val value: Double) : JvmNativeDowncallReturn
    data class ObjectHandle(val handle: JvmJniHandleId?) : JvmNativeDowncallReturn
    data class ThrownGuestException(val throwableHandle: JvmJniHandleId) : JvmNativeDowncallReturn
}

fun JvmNativeDowncallReturn.toGuestValue(environment: JvmSimulatedJniEnvironment): JvmValue? {
    val value = when (this) {
        JvmNativeDowncallReturn.Void -> null
        is JvmNativeDowncallReturn.BooleanPrimitive -> JvmBooleanValue(value)
        is JvmNativeDowncallReturn.BytePrimitive -> JvmByteValue(value)
        is JvmNativeDowncallReturn.CharPrimitive -> JvmCharValue(value)
        is JvmNativeDowncallReturn.ShortPrimitive -> JvmShortValue(value)
        is JvmNativeDowncallReturn.IntPrimitive -> JvmIntValue(value)
        is JvmNativeDowncallReturn.LongPrimitive -> JvmLongValue(value)
        is JvmNativeDowncallReturn.FloatPrimitive -> JvmFloatValue(value)
        is JvmNativeDowncallReturn.DoublePrimitive -> JvmDoubleValue(value)
        is JvmNativeDowncallReturn.ObjectHandle -> handle?.let(environment.handles::resolveObject) ?: JvmNullValue
        is JvmNativeDowncallReturn.ThrownGuestException ->
            throw JvmNativeGuestException(environment.handles.resolveObject(throwableHandle))
    }
    environment.takePendingException()?.let { throwable ->
        throw JvmNativeGuestException(throwable)
    }
    return value
}

class JvmNativeGuestException(val throwable: JvmObjectReferenceValue) : RuntimeException(
    "Native downcall threw guest exception reference ${throwable.referenceId.value}",
)

fun JvmNativeDowncallTarget.prepareInvocation(
    environment: JvmSimulatedJniEnvironment,
    guestArguments: List<JvmValue> = emptyList(),
): JvmNativeDowncallInvocation {
    require(guestMethod != null) { "JNI export invocation requires a guest method target" }
    return JvmNativeDowncallInvocation(
        target = this,
        arguments = listOf(JvmNativeDowncallArgument.SimulatedJniEnv(environment)) +
            guestArguments.map { value -> value.toDowncallArgument(environment) },
    )
}

fun JvmNativeDowncallTarget.prepareInstanceInvocation(
    environment: JvmSimulatedJniEnvironment,
    receiver: JvmObjectReferenceValue,
    guestArguments: List<JvmValue> = emptyList(),
): JvmNativeDowncallInvocation {
    val method = requireNotNull(guestMethod) { "JNI instance export invocation requires a guest method target" }
    require(!method.isStatic) { "JNI instance export invocation requires a non-static guest method target" }
    return JvmNativeDowncallInvocation(
        target = this,
        arguments = listOf(
            JvmNativeDowncallArgument.SimulatedJniEnv(environment),
            receiver.toDowncallArgument(environment),
        ) + guestArguments.map { value -> value.toDowncallArgument(environment) },
    )
}

fun JvmNativeDowncallTarget.prepareStaticInvocation(
    environment: JvmSimulatedJniEnvironment,
    classHandle: JvmJniHandleId,
    guestArguments: List<JvmValue> = emptyList(),
): JvmNativeDowncallInvocation {
    val method = requireNotNull(guestMethod) { "JNI static export invocation requires a guest method target" }
    require(method.isStatic) { "JNI static export invocation requires a static guest method target" }
    return JvmNativeDowncallInvocation(
        target = this,
        arguments = listOf(
            JvmNativeDowncallArgument.SimulatedJniEnv(environment),
            JvmNativeDowncallArgument.ClassHandle(classHandle),
        ) + guestArguments.map { value -> value.toDowncallArgument(environment) },
    )
}

private fun JvmValue.toDowncallArgument(environment: JvmSimulatedJniEnvironment): JvmNativeDowncallArgument =
    when (this) {
        is JvmBooleanValue -> JvmNativeDowncallArgument.BooleanPrimitive(value)
        is JvmByteValue -> JvmNativeDowncallArgument.BytePrimitive(value)
        is JvmCharValue -> JvmNativeDowncallArgument.CharPrimitive(value)
        is JvmShortValue -> JvmNativeDowncallArgument.ShortPrimitive(value)
        is JvmIntValue -> JvmNativeDowncallArgument.IntPrimitive(value)
        is JvmLongValue -> JvmNativeDowncallArgument.LongPrimitive(value)
        is JvmFloatValue -> JvmNativeDowncallArgument.FloatPrimitive(value)
        is JvmDoubleValue -> JvmNativeDowncallArgument.DoublePrimitive(value)
        is JvmObjectReferenceValue -> JvmNativeDowncallArgument.ObjectHandle(environment.handles.newObjectHandle(this))
        JvmNullValue -> JvmNativeDowncallArgument.ObjectHandle(null)
        else -> JvmNativeDowncallArgument.GuestValue(this)
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
