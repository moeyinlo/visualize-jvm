package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.ConstantDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolFormatException
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry

data class JvmDynamicConstantSpec(
    val constantPoolIndex: JvmRuntimeConstantPoolIndex,
    val bootstrapMethodIndex: Int,
    val name: String,
    val descriptor: String,
) {
    init {
        require(bootstrapMethodIndex >= 0) { "dynamic constant bootstrap method index must be non-negative" }
        require(name.isNotBlank()) { "dynamic constant name must not be blank" }
        require(descriptor.isNotBlank()) { "dynamic constant descriptor must not be blank" }
    }
}

data class JvmDynamicConstantLinkageSpec(
    val constant: JvmDynamicConstantSpec,
    val bootstrapMethod: JvmBootstrapMethod,
)

data class JvmDynamicConstantBootstrapInvocation(
    val constant: JvmDynamicConstantSpec,
    val bootstrapMethodHandle: JvmMethodHandlePayload,
    val staticArguments: List<JvmBootstrapArgument>,
) {
    fun materializeBootstrapMethodArguments(
        heap: JvmHeap,
        lookupClassName: String,
    ): List<JvmValue> =
        buildList {
            add(heap.internMethodHandlesLookup(lookupClassName))
            add(heap.internString(constant.name))
            add(heap.internClassMirror(constant.descriptor.dynamicConstantClassMirrorName()))
            addAll(staticArguments.map { argument -> argument.materializeDynamicBootstrapArgument(heap) })
        }
}

object JvmDynamicConstantResolver {
    fun resolveSpec(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
    ): JvmDynamicConstantSpec {
        val dynamicEntry = constantPoolEntry(constantPool, index)
        if (dynamicEntry !is ConstantDynamicEntry) {
            throw JvmDynamicConstantLinkageException(
                "dynamic constant pool index $index expected CONSTANT_Dynamic_info but found " +
                    dynamicEntry.javaClass.simpleName,
            )
        }
        val nameAndDescriptor = nameAndDescriptor(
            constantPool = constantPool,
            index = dynamicEntry.nameAndTypeIndex,
            role = "dynamic constant name_and_type_index",
        )
        return JvmDynamicConstantSpec(
            constantPoolIndex = JvmRuntimeConstantPoolIndex(index.value),
            bootstrapMethodIndex = dynamicEntry.bootstrapMethodIndex.value,
            name = nameAndDescriptor.name,
            descriptor = nameAndDescriptor.descriptor,
        )
    }

    fun resolveLinkageSpec(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        bootstrapMethods: JvmBootstrapMethodTable,
    ): JvmDynamicConstantLinkageSpec {
        val spec = resolveSpec(constantPool, index)
        return JvmDynamicConstantLinkageSpec(
            constant = spec,
            bootstrapMethod = bootstrapMethods[spec.bootstrapMethodIndex],
        )
    }

    fun resolveBootstrapInvocation(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        bootstrapMethods: JvmBootstrapMethodTable,
    ): JvmDynamicConstantBootstrapInvocation {
        val linkageSpec = resolveLinkageSpec(constantPool, index, bootstrapMethods)
        return try {
            JvmDynamicConstantBootstrapInvocation(
                constant = linkageSpec.constant,
                bootstrapMethodHandle = JvmInvokeDynamicCallSiteResolver.resolveMethodHandle(
                    constantPool = constantPool,
                    index = linkageSpec.bootstrapMethod.bootstrapMethodRef,
                    role = "dynamic constant bootstrap_method_ref",
                ),
                staticArguments = linkageSpec.bootstrapMethod.bootstrapArguments.map { argumentIndex ->
                    JvmInvokeDynamicCallSiteResolver.resolveBootstrapArgument(
                        constantPool = constantPool,
                        index = argumentIndex,
                        bootstrapKind = "dynamic constant",
                    )
                },
            )
        } catch (exception: JvmInvokeDynamicLinkageException) {
            throw JvmDynamicConstantLinkageException(exception.message ?: "Invalid dynamic constant bootstrap linkage")
        }
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
            throw JvmDynamicConstantLinkageException(
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
            throw JvmDynamicConstantLinkageException(
                "$role $index expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return entry.value
    }

    private fun constantPoolEntry(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
    ): ConstantPoolEntry = try {
        constantPool[index]
    } catch (exception: ConstantPoolFormatException) {
        throw JvmDynamicConstantLinkageException("Invalid dynamic constant pool index $index: ${exception.message}")
    }
}

class JvmDynamicConstantRegistry {
    private val resolvedConstants = linkedMapOf<JvmRuntimeConstantPoolIndex, JvmValue>()

    fun resolved(index: JvmRuntimeConstantPoolIndex): JvmValue? = resolvedConstants[index]

    fun bind(
        index: JvmRuntimeConstantPoolIndex,
        value: JvmValue,
    ): JvmValue {
        val existing = resolvedConstants[index]
        if (existing != null) {
            if (existing != value) {
                throw JvmDynamicConstantLinkageException("dynamic constant $index is already resolved")
            }
            return existing
        }
        resolvedConstants[index] = value
        return value
    }
}

private fun JvmBootstrapArgument.materializeDynamicBootstrapArgument(heap: JvmHeap): JvmValue =
    when (this) {
        is JvmBootstrapArgument.ClassConstant -> heap.internClassMirror(internalName)
        is JvmBootstrapArgument.DoubleConstant -> value
        is JvmBootstrapArgument.DynamicConstant -> throw JvmDynamicConstantLinkageException(
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

private fun String.dynamicConstantClassMirrorName(): String =
    when (this) {
        "Z" -> "boolean"
        "B" -> "byte"
        "C" -> "char"
        "S" -> "short"
        "I" -> "int"
        "J" -> "long"
        "F" -> "float"
        "D" -> "double"
        else -> when {
            startsWith("L") && endsWith(";") && length > 2 -> substring(1, lastIndex)
            startsWith("[") && isArrayFieldDescriptor() -> this
            else -> throw JvmDynamicConstantLinkageException(
                "dynamic constant descriptor $this is not a field descriptor",
            )
        }
    }

private fun String.isArrayFieldDescriptor(): Boolean {
    var componentStart = 0
    while (componentStart < length && this[componentStart] == '[') {
        componentStart += 1
    }
    if (componentStart == 0 || componentStart >= length) {
        return false
    }
    val component = substring(componentStart)
    return component in setOf("Z", "B", "C", "S", "I", "J", "F", "D") ||
        (component.startsWith("L") && component.endsWith(";") && component.length > 2)
}

class JvmDynamicConstantLinkageException(message: String) : IllegalStateException(message)
