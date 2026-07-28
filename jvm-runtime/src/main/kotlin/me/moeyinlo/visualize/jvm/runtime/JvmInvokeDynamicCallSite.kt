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
import me.moeyinlo.visualize.jvm.classfile.ClassFileFormatException
import me.moeyinlo.visualize.jvm.classfile.ClassNameValidator
import me.moeyinlo.visualize.jvm.classfile.DescriptorValidator
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
    val targetMethodHandle: JvmMethodHandlePayload,
    val target: JvmMethodHandleTarget,
) {
    constructor(
        spec: JvmInvokeDynamicCallSiteSpec,
        targetMethodHandle: JvmMethodHandlePayload,
        targetMethod: JvmResolvedMethod,
    ) : this(
        spec = spec,
        targetMethodHandle = targetMethodHandle,
        target = JvmMethodHandleTarget.Method(targetMethod),
    )

    val targetMethod: JvmResolvedMethod
        get() = (target as JvmMethodHandleTarget.Method).method
}

sealed interface JvmMethodHandleTarget {
    data class Method(val method: JvmResolvedMethod) : JvmMethodHandleTarget
    data class Field(val field: JvmResolvedField) : JvmMethodHandleTarget
}

object JvmInvokeDynamicCallSiteResolver {
    fun bindBootstrapResult(
        key: JvmInvokeDynamicCallSiteKey,
        constantPool: ConstantPool,
        classHierarchy: JvmClassHierarchy,
        invocation: JvmInvokeDynamicBootstrapInvocation,
        bootstrapResult: JvmInvokeDynamicBootstrapResult,
        registry: JvmInvokeDynamicCallSiteRegistry,
    ): JvmLinkedInvokeDynamicCallSite {
        val target = resolveMethodHandleTarget(
            constantPool = constantPool,
            classHierarchy = classHierarchy,
            methodHandle = bootstrapResult.targetMethodHandlePayload,
        )
        return registry.bind(
            key = key,
            callSite = JvmLinkedInvokeDynamicCallSite(
                spec = invocation.callSite,
                targetMethodHandle = bootstrapResult.targetMethodHandlePayload,
                target = target,
            ),
        )
    }

    fun resolveMethodHandleTarget(
        constantPool: ConstantPool,
        classHierarchy: JvmClassHierarchy,
        methodHandle: JvmMethodHandlePayload,
    ): JvmMethodHandleTarget =
        when (methodHandle.referenceKind) {
            JvmMethodHandleReferenceKind.GetField -> JvmMethodHandleTarget.Field(
                resolveFieldMethodHandleTarget(
                    constantPool = constantPool,
                    classHierarchy = classHierarchy,
                    methodHandle = methodHandle,
                    operationName = "GetField",
                    expectStatic = false,
                ),
            )
            JvmMethodHandleReferenceKind.PutField -> JvmMethodHandleTarget.Field(
                resolveFieldMethodHandleTarget(
                    constantPool = constantPool,
                    classHierarchy = classHierarchy,
                    methodHandle = methodHandle,
                    operationName = "PutField",
                    expectStatic = false,
                ),
            )
            JvmMethodHandleReferenceKind.GetStatic -> JvmMethodHandleTarget.Field(
                resolveFieldMethodHandleTarget(
                    constantPool = constantPool,
                    classHierarchy = classHierarchy,
                    methodHandle = methodHandle,
                    operationName = "GetStatic",
                    expectStatic = true,
                ),
            )
            JvmMethodHandleReferenceKind.PutStatic -> JvmMethodHandleTarget.Field(
                resolveFieldMethodHandleTarget(
                    constantPool = constantPool,
                    classHierarchy = classHierarchy,
                    methodHandle = methodHandle,
                    operationName = "PutStatic",
                    expectStatic = true,
                ),
            )

            JvmMethodHandleReferenceKind.InvokeStatic,
            JvmMethodHandleReferenceKind.InvokeVirtual,
            JvmMethodHandleReferenceKind.InvokeSpecial,
            JvmMethodHandleReferenceKind.InvokeInterface,
            JvmMethodHandleReferenceKind.NewInvokeSpecial,
            -> JvmMethodHandleTarget.Method(
                resolveMethodHandleTargetMethod(
                    constantPool = constantPool,
                    classHierarchy = classHierarchy,
                    methodHandle = methodHandle,
                ),
            )
        }

    fun resolveMethodHandleTargetMethod(
        constantPool: ConstantPool,
        classHierarchy: JvmClassHierarchy,
        methodHandle: JvmMethodHandlePayload,
    ): JvmResolvedMethod {
        val referenceIndex = ConstantPoolIndex(methodHandle.referenceIndex)
        val methodReferenceEntry = constantPoolEntry(constantPool, referenceIndex)
        when (methodHandle.referenceKind) {
            JvmMethodHandleReferenceKind.InvokeStatic -> {
                if (methodReferenceEntry !is ConstantMethodRefEntry && methodReferenceEntry !is ConstantInterfaceMethodRefEntry) {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle InvokeStatic reference index $referenceIndex expected method reference but found " +
                            methodReferenceEntry.javaClass.simpleName,
                    )
                }
                val resolvedMethod = resolveMethodHandleReferenceMethod(
                    constantPool = constantPool,
                    classHierarchy = classHierarchy,
                    methodReferenceEntry = methodReferenceEntry,
                    referenceIndex = referenceIndex,
                )
                rejectInitializationMethodHandleTarget("InvokeStatic", resolvedMethod)
                if (!resolvedMethod.isStatic) {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle InvokeStatic target ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                            "${resolvedMethod.descriptor} resolved to a non-static method",
                    )
                }
                return resolvedMethod
            }

            JvmMethodHandleReferenceKind.InvokeVirtual -> {
                if (methodReferenceEntry !is ConstantMethodRefEntry) {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle InvokeVirtual reference index $referenceIndex expected class method reference " +
                            "but found ${methodReferenceEntry.javaClass.simpleName}",
                    )
                }
                val resolvedMethod = resolveMethodHandleReferenceMethod(
                    constantPool = constantPool,
                    classHierarchy = classHierarchy,
                    methodReferenceEntry = methodReferenceEntry,
                    referenceIndex = referenceIndex,
                )
                rejectInitializationMethodHandleTarget("InvokeVirtual", resolvedMethod)
                if (resolvedMethod.isStatic) {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle InvokeVirtual target ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                            "${resolvedMethod.descriptor} resolved to a static method",
                    )
                }
                return resolvedMethod
            }

            JvmMethodHandleReferenceKind.InvokeSpecial -> {
                if (methodReferenceEntry !is ConstantMethodRefEntry && methodReferenceEntry !is ConstantInterfaceMethodRefEntry) {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle InvokeSpecial reference index $referenceIndex expected method reference but found " +
                            methodReferenceEntry.javaClass.simpleName,
                    )
                }
                val resolvedMethod = resolveMethodHandleReferenceMethod(
                    constantPool = constantPool,
                    classHierarchy = classHierarchy,
                    methodReferenceEntry = methodReferenceEntry,
                    referenceIndex = referenceIndex,
                )
                rejectInitializationMethodHandleTarget("InvokeSpecial", resolvedMethod)
                if (resolvedMethod.isStatic) {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle InvokeSpecial target ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                            "${resolvedMethod.descriptor} resolved to a static method",
                    )
                }
                return resolvedMethod
            }

            JvmMethodHandleReferenceKind.InvokeInterface -> {
                if (methodReferenceEntry !is ConstantInterfaceMethodRefEntry) {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle InvokeInterface reference index $referenceIndex expected interface method " +
                            "reference but found ${methodReferenceEntry.javaClass.simpleName}",
                    )
                }
                val methodReference = methodReference(
                    constantPool = constantPool,
                    entry = methodReferenceEntry,
                    referenceIndex = referenceIndex,
                )
                val resolvedMethod = classHierarchy.resolveInterfaceMethod(
                    ownerClassName = methodReference.ownerClassName,
                    name = methodReference.name,
                    descriptor = methodReference.descriptor,
                )
                rejectInitializationMethodHandleTarget("InvokeInterface", resolvedMethod)
                if (resolvedMethod.isStatic) {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle InvokeInterface target ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                            "${resolvedMethod.descriptor} resolved to a static method",
                    )
                }
                return resolvedMethod
            }

            JvmMethodHandleReferenceKind.NewInvokeSpecial -> {
                if (methodReferenceEntry !is ConstantMethodRefEntry) {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle NewInvokeSpecial reference index $referenceIndex expected class method " +
                            "reference but found ${methodReferenceEntry.javaClass.simpleName}",
                    )
                }
                val resolvedMethod = resolveMethodHandleReferenceMethod(
                    constantPool = constantPool,
                    classHierarchy = classHierarchy,
                    methodReferenceEntry = methodReferenceEntry,
                    referenceIndex = referenceIndex,
                )
                if (resolvedMethod.name != "<init>") {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle NewInvokeSpecial target ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                            "${resolvedMethod.descriptor} is not an instance initializer",
                    )
                }
                if (resolvedMethod.descriptor.substringAfterLast(')') != "V") {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle NewInvokeSpecial target ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                            "${resolvedMethod.descriptor} must return void",
                    )
                }
                if (resolvedMethod.isStatic) {
                    throw JvmInvokeDynamicLinkageException(
                        "MethodHandle NewInvokeSpecial target ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                            "${resolvedMethod.descriptor} resolved to a static method",
                    )
                }
                return resolvedMethod
            }

            else -> throw JvmInvokeDynamicLinkageException(
                "MethodHandle reference kind ${methodHandle.referenceKind} target resolution is not implemented yet",
            )
        }
    }

    private fun rejectInitializationMethodHandleTarget(
        operationName: String,
        resolvedMethod: JvmResolvedMethod,
    ) {
        if (resolvedMethod.name == "<init>" || resolvedMethod.name == "<clinit>") {
            throw JvmInvokeDynamicLinkageException(
                "MethodHandle $operationName target ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} must not target an initialization method",
            )
        }
    }

    private fun resolveFieldMethodHandleTarget(
        constantPool: ConstantPool,
        classHierarchy: JvmClassHierarchy,
        methodHandle: JvmMethodHandlePayload,
        operationName: String,
        expectStatic: Boolean,
    ): JvmResolvedField {
        val referenceIndex = ConstantPoolIndex(methodHandle.referenceIndex)
        val fieldReferenceEntry = constantPoolEntry(constantPool, referenceIndex)
        if (fieldReferenceEntry !is ConstantFieldRefEntry) {
            throw JvmInvokeDynamicLinkageException(
                "MethodHandle $operationName reference index $referenceIndex expected field reference but found " +
                    fieldReferenceEntry.javaClass.simpleName,
            )
        }
        val resolvedField = resolveMethodHandleReferenceField(
            constantPool = constantPool,
            classHierarchy = classHierarchy,
            fieldReferenceEntry = fieldReferenceEntry,
            referenceIndex = referenceIndex,
        )
        if (expectStatic && !resolvedField.isStatic) {
            throw JvmInvokeDynamicLinkageException(
                "MethodHandle $operationName target ${resolvedField.ownerClassName}.${resolvedField.name}:" +
                    "${resolvedField.descriptor} resolved to a non-static field",
            )
        }
        if (!expectStatic && resolvedField.isStatic) {
            throw JvmInvokeDynamicLinkageException(
                "MethodHandle $operationName target ${resolvedField.ownerClassName}.${resolvedField.name}:" +
                    "${resolvedField.descriptor} resolved to a static field",
            )
        }
        return resolvedField
    }

    private fun resolveMethodHandleReferenceMethod(
        constantPool: ConstantPool,
        classHierarchy: JvmClassHierarchy,
        methodReferenceEntry: ConstantPoolEntry,
        referenceIndex: ConstantPoolIndex,
    ): JvmResolvedMethod {
        val methodReference = methodReference(constantPool, methodReferenceEntry, referenceIndex)
        return classHierarchy.resolveMethod(
            ownerClassName = methodReference.ownerClassName,
            name = methodReference.name,
            descriptor = methodReference.descriptor,
        )
    }

    private fun resolveMethodHandleReferenceField(
        constantPool: ConstantPool,
        classHierarchy: JvmClassHierarchy,
        fieldReferenceEntry: ConstantFieldRefEntry,
        referenceIndex: ConstantPoolIndex,
    ): JvmResolvedField {
        val fieldReference = fieldReference(constantPool, fieldReferenceEntry, referenceIndex)
        return classHierarchy.resolveField(
            ownerClassName = fieldReference.ownerClassName,
            name = fieldReference.name,
            descriptor = fieldReference.descriptor,
        )
    }

    private data class FieldReference(
        val ownerClassName: String,
        val name: String,
        val descriptor: String,
    )

    private fun fieldReference(
        constantPool: ConstantPool,
        entry: ConstantFieldRefEntry,
        referenceIndex: ConstantPoolIndex,
    ): FieldReference {
        val classEntry = constantPoolEntry(constantPool, entry.classIndex)
        if (classEntry !is ConstantClassEntry) {
            throw JvmInvokeDynamicLinkageException(
                "MethodHandle field reference index $referenceIndex class_index ${entry.classIndex} " +
                    "expected CONSTANT_Class_info but found ${classEntry.javaClass.simpleName}",
            )
        }
        val nameAndDescriptor = nameAndDescriptor(
            constantPool = constantPool,
            index = entry.nameAndTypeIndex,
            role = "MethodHandle field reference name_and_type_index",
        )
        return FieldReference(
            ownerClassName = utf8Value(constantPool, classEntry.nameIndex, "MethodHandle field reference class name_index"),
            name = nameAndDescriptor.name,
            descriptor = nameAndDescriptor.descriptor,
        )
    }

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
            name = nameEntry.value.requireInvokeDynamicMethodName(index),
            descriptor = descriptorEntry.value.requireInvokeDynamicMethodDescriptor(index),
        )
    }

    private fun String.requireInvokeDynamicMethodName(owner: ConstantPoolIndex): String {
        try {
            ClassNameValidator.validateMethodName(
                owner = owner,
                role = "invokedynamic call site name",
                value = this,
                allowInit = false,
            )
            return this
        } catch (_: ClassFileFormatException) {
            throw JvmInvokeDynamicLinkageException("invokedynamic call site name $this is not a valid method name")
        }
    }

    private fun String.requireInvokeDynamicMethodDescriptor(owner: ConstantPoolIndex): String {
        try {
            DescriptorValidator.validateMethodDescriptor(
                owner = owner,
                role = "invokedynamic call site descriptor",
                descriptor = this,
            )
            return this
        } catch (_: ClassFileFormatException) {
            throw JvmInvokeDynamicLinkageException("invokedynamic call site descriptor $this is not a method descriptor")
        }
    }

    internal fun resolveBootstrapArgument(
        constantPool: ConstantPool,
        index: JvmRuntimeConstantPoolIndex,
        bootstrapKind: String = "invokedynamic",
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
                    descriptor = nameAndDescriptor.descriptor.requireBootstrapDynamicArgumentFieldDescriptor(
                        bootstrapKind = bootstrapKind,
                        owner = constantPoolIndex,
                    ),
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
                descriptor = utf8Value(constantPool, entry.descriptorIndex, "bootstrap method type descriptor_index")
                    .requireBootstrapMethodTypeDescriptor(bootstrapKind, constantPoolIndex),
            )
            is ConstantStringEntry -> JvmBootstrapArgument.StringConstant(
                constantPoolIndex = index,
                value = utf8Value(constantPool, entry.stringIndex, "bootstrap string argument string_index"),
            )
            else -> throw JvmInvokeDynamicLinkageException(
                "$bootstrapKind bootstrap argument index $constantPoolIndex expected a loadable constant but found " +
                    entry.javaClass.simpleName,
            )
        }
    }

    private fun String.requireBootstrapMethodTypeDescriptor(
        bootstrapKind: String,
        owner: ConstantPoolIndex,
    ): String {
        try {
            DescriptorValidator.validateMethodDescriptor(
                owner = owner,
                role = "$bootstrapKind bootstrap method type descriptor",
                descriptor = this,
            )
            return this
        } catch (_: ClassFileFormatException) {
            throw JvmInvokeDynamicLinkageException(
                "$bootstrapKind bootstrap method type descriptor $this is not a method descriptor",
            )
        }
    }
    private fun String.requireBootstrapDynamicArgumentFieldDescriptor(
        bootstrapKind: String,
        owner: ConstantPoolIndex,
    ): String {
        try {
            DescriptorValidator.validateFieldDescriptor(
                owner = owner,
                role = "$bootstrapKind bootstrap dynamic argument descriptor",
                descriptor = this,
            )
            return this
        } catch (_: ClassFileFormatException) {
            throw JvmInvokeDynamicLinkageException(
                "$bootstrapKind bootstrap dynamic argument descriptor $this is not a field descriptor",
            )
        }
    }

    internal fun resolveMethodHandle(
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
        validateMethodHandleTarget(
            constantPool = constantPool,
            referencedEntry = referencedEntry,
            referenceKind = entry.referenceKind,
            role = "$role index $constantPoolIndex target",
        )
        return JvmMethodHandlePayload(
            referenceKind = entry.referenceKind.toRuntimeReferenceKind(),
            referenceIndex = entry.referenceIndex.value,
        )
    }

    private fun validateMethodHandleTarget(
        constantPool: ConstantPool,
        referencedEntry: ConstantPoolEntry,
        referenceKind: MethodHandleReferenceKind,
        role: String,
    ) {
        val nameAndTypeIndex = when (referencedEntry) {
            is ConstantFieldRefEntry -> {
                val nameAndDescriptor = nameAndDescriptor(
                    constantPool = constantPool,
                    index = referencedEntry.nameAndTypeIndex,
                    role = "$role name_and_type_index",
                )
                nameAndDescriptor.name.requireMethodHandleFieldName(
                    owner = referencedEntry.nameAndTypeIndex,
                    role = role,
                )
                try {
                    DescriptorValidator.validateFieldDescriptor(
                        owner = referencedEntry.nameAndTypeIndex,
                        role = "$role descriptor",
                        descriptor = nameAndDescriptor.descriptor,
                    )
                } catch (_: ClassFileFormatException) {
                    throw JvmInvokeDynamicLinkageException(
                        "$role descriptor ${nameAndDescriptor.descriptor} is not a field descriptor",
                    )
                }
                return
            }
            is ConstantMethodRefEntry -> referencedEntry.nameAndTypeIndex
            is ConstantInterfaceMethodRefEntry -> referencedEntry.nameAndTypeIndex
            else -> return
        }
        val nameAndDescriptor = nameAndDescriptor(
            constantPool = constantPool,
            index = nameAndTypeIndex,
            role = "$role name_and_type_index",
        )
        try {
            DescriptorValidator.validateMethodDescriptor(
                owner = nameAndTypeIndex,
                role = "$role descriptor",
                descriptor = nameAndDescriptor.descriptor,
            )
        } catch (_: ClassFileFormatException) {
            throw JvmInvokeDynamicLinkageException(
                "$role descriptor ${nameAndDescriptor.descriptor} is not a method descriptor",
            )
        }
        if (referenceKind == MethodHandleReferenceKind.NewInvokeSpecial && nameAndDescriptor.name != "<init>") {
            throw JvmInvokeDynamicLinkageException(
                "$role reference_kind NewInvokeSpecial must target <init> but found ${nameAndDescriptor.name}",
            )
        }
        if (
            referenceKind == MethodHandleReferenceKind.NewInvokeSpecial &&
            nameAndDescriptor.descriptor.substringAfterLast(')') != "V"
        ) {
            throw JvmInvokeDynamicLinkageException(
                "$role reference_kind NewInvokeSpecial descriptor ${nameAndDescriptor.descriptor} must return void",
            )
        }
        if (
            referenceKind != MethodHandleReferenceKind.NewInvokeSpecial &&
            (nameAndDescriptor.name == "<init>" || nameAndDescriptor.name == "<clinit>")
        ) {
            throw JvmInvokeDynamicLinkageException(
                "$role reference_kind $referenceKind must not target ${nameAndDescriptor.name}",
            )
        }
    }

    private fun String.requireMethodHandleFieldName(
        owner: ConstantPoolIndex,
        role: String,
    ): String {
        try {
            ClassNameValidator.validateUnqualifiedName(
                owner = owner,
                role = "$role field name",
                value = this,
            )
            return this
        } catch (_: ClassFileFormatException) {
            throw JvmInvokeDynamicLinkageException("$role field name $this is not a valid unqualified name")
        }
    }

    private data class MethodReference(
        val ownerClassName: String,
        val name: String,
        val descriptor: String,
    )

    private fun methodReference(
        constantPool: ConstantPool,
        entry: ConstantPoolEntry,
        referenceIndex: ConstantPoolIndex,
    ): MethodReference {
        val classIndex = when (entry) {
            is ConstantMethodRefEntry -> entry.classIndex
            is ConstantInterfaceMethodRefEntry -> entry.classIndex
            else -> error("Unsupported method reference entry ${entry.javaClass.simpleName}")
        }
        val nameAndTypeIndex = when (entry) {
            is ConstantMethodRefEntry -> entry.nameAndTypeIndex
            is ConstantInterfaceMethodRefEntry -> entry.nameAndTypeIndex
        }
        val classEntry = constantPoolEntry(constantPool, classIndex)
        if (classEntry !is ConstantClassEntry) {
            throw JvmInvokeDynamicLinkageException(
                "MethodHandle reference index $referenceIndex class_index $classIndex " +
                    "expected CONSTANT_Class_info but found ${classEntry.javaClass.simpleName}",
            )
        }
        val nameAndDescriptor = nameAndDescriptor(
            constantPool = constantPool,
            index = nameAndTypeIndex,
            role = "MethodHandle reference name_and_type_index",
        )
        return MethodReference(
            ownerClassName = utf8Value(constantPool, classEntry.nameIndex, "MethodHandle reference class name_index"),
            name = nameAndDescriptor.name,
            descriptor = nameAndDescriptor.descriptor,
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
