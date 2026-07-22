package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.ConstantInvokeDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolFormatException
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry

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

data class JvmLinkedInvokeDynamicCallSite(
    val spec: JvmInvokeDynamicCallSiteSpec,
    val targetMethod: JvmResolvedMethod,
)

object JvmInvokeDynamicCallSiteResolver {
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