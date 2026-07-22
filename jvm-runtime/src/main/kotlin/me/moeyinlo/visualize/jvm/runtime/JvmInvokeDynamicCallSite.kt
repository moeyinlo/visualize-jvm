package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFieldRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantInterfaceMethodRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantInvokeDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodHandleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolFormatException
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import me.moeyinlo.visualize.jvm.classfile.MethodHandleReferenceKind

data class JvmInvokeDynamicCallSiteKey(
    val ownerClassName: String,
    val bytecodeOffset: Int,
) {
    init {
        require(ownerClassName.isNotBlank()) { "invokedynamic owner class name must not be blank" }
        require(bytecodeOffset >= 0) { "invokedynamic bytecode offset must be non-negative: $bytecodeOffset" }
    }
}

data class JvmInvokeDynamicCallSiteSpec(
    val constantPoolIndex: JvmRuntimeConstantPoolIndex,
    val bootstrapMethodIndex: Int,
    val name: String,
    val descriptor: String,
) {
    init {
        require(bootstrapMethodIndex >= 0) { "bootstrap method index must be non-negative: $bootstrapMethodIndex" }
        require(name.isNotBlank()) { "invokedynamic call site name must not be blank" }
        require(descriptor.isNotBlank()) { "invokedynamic call site descriptor must not be blank" }
    }
}

data class JvmInvokeDynamicLinkageSpec(
    val callSite: JvmInvokeDynamicCallSiteSpec,
    val bootstrapMethod: JvmBootstrapMethod,
)

data class JvmInvokeDynamicBootstrapInvocation(
    val callSite: JvmInvokeDynamicCallSiteSpec,
    val bootstrapMethodHandle: JvmMethodHandlePayload,
    val staticArguments: List<JvmBootstrapArgument>,
) {
    fun materializeBootstrapMethodArguments(
        heap: JvmHeap,
        lookupClassName: String,
    ): List<JvmValue> =
        buildList {
            add(heap.internMethodHandlesLookup(lookupClassName))
            add(heap.internString(callSite.name))
            add(heap.internMethodType(callSite.descriptor))
            addAll(staticArguments.map { argument -> argument.materialize(heap) })
        }

    fun extractBootstrapResult(
        heap: JvmHeap,
        returnValue: JvmValue,
    ): JvmInvokeDynamicBootstrapResult {
        val callSiteReference = returnValue as? JvmObjectReferenceValue
            ?: throw JvmInvokeDynamicLinkageException(
                "invokedynamic bootstrap method must return a java/lang/invoke/CallSite reference but returned " +
                    returnValue.javaClass.simpleName,
            )
        val callSiteObject = heap.get(callSiteReference)
        if (callSiteObject.className != "java/lang/invoke/CallSite") {
            throw JvmInvokeDynamicLinkageException(
                "invokedynamic bootstrap method returned ${callSiteObject.className}, expected java/lang/invoke/CallSite",
            )
        }
        val callSitePayload = callSiteObject.payload as? JvmCallSitePayload
            ?: throw JvmInvokeDynamicLinkageException(
                "invokedynamic bootstrap method returned CallSite without a target MethodHandle payload",
            )
        val targetPayload = heap.get(callSitePayload.targetMethodHandle).payload as? JvmMethodHandlePayload
            ?: throw JvmInvokeDynamicLinkageException(
                "invokedynamic bootstrap method returned CallSite target without MethodHandle payload",
            )
        return JvmInvokeDynamicBootstrapResult(
            callSiteReference = callSiteReference,
            targetMethodHandle = callSitePayload.targetMethodHandle,
            targetMethodHandlePayload = targetPayload,
        )
    }
}

data class JvmInvokeDynamicBootstrapResult(
    val callSiteReference: JvmObjectReferenceValue,
    val targetMethodHandle: JvmObjectReferenceValue,
    val targetMethodHandlePayload: JvmMethodHandlePayload,
)

sealed interface JvmBootstrapArgument {
    val constantPoolIndex: JvmRuntimeConstantPoolIndex

    data class IntegerConstant(
        override val constantPoolIndex: JvmRuntimeConstantPoolIndex,
        val value: JvmIntValue,
    ) : JvmBootstrapArgument

    data class FloatConstant(
        override val constantPoolIndex: JvmRuntimeConstantPoolIndex,
        val value: JvmFloatValue,
    ) : JvmBootstrapArgument

    data class LongConstant(
        override val constantPoolIndex: JvmRuntimeConstantPoolIndex,
        val value: JvmLongValue,
    ) : JvmBootstrapArgument

    data class DoubleConstant(
        override val constantPoolIndex: JvmRuntimeConstantPoolIndex,
        val value: JvmDoubleValue,
    ) : JvmBootstrapArgument

    data class StringConstant(
        override val constantPoolIndex: JvmRuntimeConstantPoolIndex,
        val value: String,
    ) : JvmBootstrapArgument

    data class ClassConstant(
        override val constantPoolIndex: JvmRuntimeConstantPoolIndex,
        val internalName: String,
    ) : JvmBootstrapArgument

    data class MethodTypeConstant(
        override val constantPoolIndex: JvmRuntimeConstantPoolIndex,
        val descriptor: String,
    ) : JvmBootstrapArgument

    data class MethodHandleConstant(
        override val constantPoolIndex: JvmRuntimeConstantPoolIndex,
        val payload: JvmMethodHandlePayload,
    ) : JvmBootstrapArgument

    data class DynamicConstant(
        override val constantPoolIndex: JvmRuntimeConstantPoolIndex,
        val bootstrapMethodIndex: Int,
        val name: String,
        val descriptor: String,
    ) : JvmBootstrapArgument
}

private fun JvmBootstrapArgument.materialize(heap: JvmHeap): JvmValue =
    when (this) {
        is JvmBootstrapArgument.ClassConstant -> heap.internClassMirror(internalName)
        is JvmBootstrapArgument.DoubleConstant -> value
        is JvmBootstrapArgument.DynamicConstant -> throw JvmInvokeDynamicLinkageException(
            "CONSTANT_Dynamic bootstrap argument $constantPoolIndex requires dynamic-constant resolution",
        )
        is JvmBootstrapArgument.FloatConstant -> value
        is JvmBootstrapArgument.IntegerConstant -> value
        is JvmBootstrapArgument.LongConstant -> value
        is JvmBootstrapArgument.MethodHandleConstant -> heap.internMethodHandle(
            referenceKind = payload.referenceKind,
            referenceIndex = payload.referenceIndex,
        )
        is JvmBootstrapArgument.MethodTypeConstant -> heap.internMethodType(descriptor)
        is JvmBootstrapArgument.StringConstant -> heap.internString(value)
    }

data class JvmLinkedInvokeDynamicCallSite(
    val spec: JvmInvokeDynamicCallSiteSpec,
    val targetMethod: JvmResolvedMethod,
)

object JvmInvokeDynamicCallSiteResolver {
    fun resolveBootstrapInvocation(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        bootstrapMethods: JvmBootstrapMethodTable,
    ): JvmInvokeDynamicBootstrapInvocation {
        val linkageSpec = resolveLinkageSpec(constantPool, index, bootstrapMethods)
        return JvmInvokeDynamicBootstrapInvocation(
            callSite = linkageSpec.callSite,
            bootstrapMethodHandle = resolveMethodHandle(
                constantPool = constantPool,
                index = linkageSpec.bootstrapMethod.bootstrapMethodRef,
                role = "invokedynamic bootstrap_method_ref",
            ),
            staticArguments = linkageSpec.bootstrapMethod.bootstrapArguments.map { argumentIndex ->
                resolveBootstrapArgument(constantPool, argumentIndex)
            },
        )
    }

    fun resolveLinkageSpec(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        bootstrapMethods: JvmBootstrapMethodTable,
    ): JvmInvokeDynamicLinkageSpec {
        val callSiteSpec = resolveSpec(constantPool, index)
        return JvmInvokeDynamicLinkageSpec(
            callSite = callSiteSpec,
            bootstrapMethod = bootstrapMethods[callSiteSpec.bootstrapMethodIndex],
        )
    }

    fun resolveSpec(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
    ): JvmInvokeDynamicCallSiteSpec {
        val invokeDynamicEntry = constantPoolEntry(constantPool, index)
        if (invokeDynamicEntry !is ConstantInvokeDynamicEntry) {
            throw JvmInvokeDynamicLinkageException(
                "invokedynamic constant pool index $index expected CONSTANT_InvokeDynamic_info but found " +
                    invokeDynamicEntry.javaClass.simpleName,
            )
        }
        val nameAndTypeEntry = constantPoolEntry(constantPool, invokeDynamicEntry.nameAndTypeIndex)
        if (nameAndTypeEntry !is ConstantNameAndTypeEntry) {
            throw JvmInvokeDynamicLinkageException(
                "invokedynamic name_and_type index ${invokeDynamicEntry.nameAndTypeIndex} " +
                    "expected CONSTANT_NameAndType_info but found ${nameAndTypeEntry.javaClass.simpleName}",
            )
        }
        val nameEntry = constantPoolEntry(constantPool, nameAndTypeEntry.nameIndex)
        if (nameEntry !is ConstantUtf8Entry) {
            throw JvmInvokeDynamicLinkageException(
                "invokedynamic call site name index ${nameAndTypeEntry.nameIndex} " +
                    "expected CONSTANT_Utf8_info but found ${nameEntry.javaClass.simpleName}",
            )
        }
        val descriptorEntry = constantPoolEntry(constantPool, nameAndTypeEntry.descriptorIndex)
        if (descriptorEntry !is ConstantUtf8Entry) {
            throw JvmInvokeDynamicLinkageException(
                "invokedynamic call site descriptor index ${nameAndTypeEntry.descriptorIndex} " +
                    "expected CONSTANT_Utf8_info but found ${descriptorEntry.javaClass.simpleName}",
            )
        }
        return JvmInvokeDynamicCallSiteSpec(
            constantPoolIndex = JvmRuntimeConstantPoolIndex(index.value),
            bootstrapMethodIndex = invokeDynamicEntry.bootstrapMethodIndex.value,
            name = nameEntry.value,
            descriptor = descriptorEntry.value,
        )
    }

    private fun resolveBootstrapArgument(
        constantPool: ConstantPool,
        index: JvmRuntimeConstantPoolIndex,
    ): JvmBootstrapArgument {
        val constantPoolIndex = ConstantPoolIndex(index.value)
        return when (val entry = constantPoolEntry(constantPool, constantPoolIndex)) {
            is ConstantClassEntry -> JvmBootstrapArgument.ClassConstant(
                constantPoolIndex = index,
                internalName = utf8Value(constantPool, entry.nameIndex, "bootstrap class argument name_index"),
            )
            is ConstantDoubleEntry -> JvmBootstrapArgument.DoubleConstant(
                constantPoolIndex = index,
                value = JvmDoubleValue(entry.value),
            )
            is ConstantDynamicEntry -> {
                val nameAndDescriptor = nameAndDescriptor(
                    constantPool = constantPool,
                    index = entry.nameAndTypeIndex,
                    role = "bootstrap dynamic argument name_and_type_index",
                )
                JvmBootstrapArgument.DynamicConstant(
                    constantPoolIndex = index,
                    bootstrapMethodIndex = entry.bootstrapMethodIndex.value,
                    name = nameAndDescriptor.name,
                    descriptor = nameAndDescriptor.descriptor,
                )
            }
            is ConstantFloatEntry -> JvmBootstrapArgument.FloatConstant(
                constantPoolIndex = index,
                value = JvmFloatValue(entry.value),
            )
            is ConstantIntegerEntry -> JvmBootstrapArgument.IntegerConstant(
                constantPoolIndex = index,
                value = JvmIntValue(entry.value),
            )
            is ConstantLongEntry -> JvmBootstrapArgument.LongConstant(
                constantPoolIndex = index,
                value = JvmLongValue(entry.value),
            )
            is ConstantMethodHandleEntry -> JvmBootstrapArgument.MethodHandleConstant(
                constantPoolIndex = index,
                payload = resolveMethodHandle(constantPool, index, "bootstrap method handle argument"),
            )
            is ConstantMethodTypeEntry -> JvmBootstrapArgument.MethodTypeConstant(
                constantPoolIndex = index,
                descriptor = utf8Value(constantPool, entry.descriptorIndex, "bootstrap method type descriptor_index"),
            )
            is ConstantStringEntry -> JvmBootstrapArgument.StringConstant(
                constantPoolIndex = index,
                value = utf8Value(constantPool, entry.stringIndex, "bootstrap string argument string_index"),
            )
            else -> throw JvmInvokeDynamicLinkageException(
                "invokedynamic bootstrap argument index $constantPoolIndex expected a loadable constant but found " +
                    entry.javaClass.simpleName,
            )
        }
    }

    private fun resolveMethodHandle(
        constantPool: ConstantPool,
        index: JvmRuntimeConstantPoolIndex,
        role: String,
    ): JvmMethodHandlePayload {
        val constantPoolIndex = ConstantPoolIndex(index.value)
        val entry = constantPoolEntry(constantPool, constantPoolIndex)
        if (entry !is ConstantMethodHandleEntry) {
            throw JvmInvokeDynamicLinkageException(
                "$role index $constantPoolIndex expected CONSTANT_MethodHandle_info but found " +
                    entry.javaClass.simpleName,
            )
        }
        val referencedEntry = constantPoolEntry(constantPool, entry.referenceIndex)
        if (!entry.referenceKind.matches(referencedEntry)) {
            throw JvmInvokeDynamicLinkageException(
                "$role index $constantPoolIndex has reference_kind ${entry.referenceKind} targeting " +
                    referencedEntry.javaClass.simpleName,
            )
        }
        return JvmMethodHandlePayload(
            referenceKind = entry.referenceKind.toRuntimeReferenceKind(),
            referenceIndex = entry.referenceIndex.value,
        )
    }

    private data class NameAndDescriptor(
        val name: String,
        val descriptor: String,
    )

    private fun nameAndDescriptor(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        role: String,
    ): NameAndDescriptor {
        val entry = constantPoolEntry(constantPool, index)
        if (entry !is ConstantNameAndTypeEntry) {
            throw JvmInvokeDynamicLinkageException(
                "$role $index expected CONSTANT_NameAndType_info but found ${entry.javaClass.simpleName}",
            )
        }
        return NameAndDescriptor(
            name = utf8Value(constantPool, entry.nameIndex, "$role name_index"),
            descriptor = utf8Value(constantPool, entry.descriptorIndex, "$role descriptor_index"),
        )
    }

    private fun utf8Value(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        role: String,
    ): String {
        val entry = constantPoolEntry(constantPool, index)
        if (entry !is ConstantUtf8Entry) {
            throw JvmInvokeDynamicLinkageException(
                "$role $index expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return entry.value
    }

    private fun MethodHandleReferenceKind.matches(entry: ConstantPoolEntry): Boolean =
        when (this) {
            MethodHandleReferenceKind.GetField,
            MethodHandleReferenceKind.GetStatic,
            MethodHandleReferenceKind.PutField,
            MethodHandleReferenceKind.PutStatic,
            -> entry is ConstantFieldRefEntry

            MethodHandleReferenceKind.InvokeVirtual,
            MethodHandleReferenceKind.NewInvokeSpecial,
            -> entry is ConstantMethodRefEntry

            MethodHandleReferenceKind.InvokeStatic,
            MethodHandleReferenceKind.InvokeSpecial,
            -> entry is ConstantMethodRefEntry || entry is ConstantInterfaceMethodRefEntry

            MethodHandleReferenceKind.InvokeInterface -> entry is ConstantInterfaceMethodRefEntry
        }

    private fun MethodHandleReferenceKind.toRuntimeReferenceKind(): JvmMethodHandleReferenceKind =
        when (this) {
            MethodHandleReferenceKind.GetField -> JvmMethodHandleReferenceKind.GetField
            MethodHandleReferenceKind.GetStatic -> JvmMethodHandleReferenceKind.GetStatic
            MethodHandleReferenceKind.PutField -> JvmMethodHandleReferenceKind.PutField
            MethodHandleReferenceKind.PutStatic -> JvmMethodHandleReferenceKind.PutStatic
            MethodHandleReferenceKind.InvokeVirtual -> JvmMethodHandleReferenceKind.InvokeVirtual
            MethodHandleReferenceKind.InvokeStatic -> JvmMethodHandleReferenceKind.InvokeStatic
            MethodHandleReferenceKind.InvokeSpecial -> JvmMethodHandleReferenceKind.InvokeSpecial
            MethodHandleReferenceKind.NewInvokeSpecial -> JvmMethodHandleReferenceKind.NewInvokeSpecial
            MethodHandleReferenceKind.InvokeInterface -> JvmMethodHandleReferenceKind.InvokeInterface
        }

    private fun constantPoolEntry(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
    ): ConstantPoolEntry = try {
        constantPool[index]
    } catch (exception: ConstantPoolFormatException) {
        throw JvmInvokeDynamicLinkageException("Invalid invokedynamic constant pool index $index: ${exception.message}")
    }
}
class JvmInvokeDynamicCallSiteRegistry {
    private val linkedCallSites = linkedMapOf<JvmInvokeDynamicCallSiteKey, JvmLinkedInvokeDynamicCallSite>()

    fun linked(key: JvmInvokeDynamicCallSiteKey): JvmLinkedInvokeDynamicCallSite? = linkedCallSites[key]

    fun bind(
        key: JvmInvokeDynamicCallSiteKey,
        callSite: JvmLinkedInvokeDynamicCallSite,
    ): JvmLinkedInvokeDynamicCallSite {
        val existing = linkedCallSites[key]
        if (existing != null) {
            if (existing != callSite) {
                throw JvmInvokeDynamicLinkageException(
                    "invokedynamic call site ${key.ownerClassName}@${key.bytecodeOffset} is already linked",
                )
            }
            return existing
        }
        linkedCallSites[key] = callSite
        return callSite
    }
}

class JvmInvokeDynamicLinkageException(message: String) : IllegalStateException(message)
