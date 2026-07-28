package me.moeyinlo.visualize.jvm.classfile

class ConstantPoolFormatException(message: String) : RuntimeException(message)

@JvmInline
value class ConstantPoolIndex(val value: Int) {
    init {
        require(value >= 1) { "Constant pool indexes are one-based: $value" }
    }

    override fun toString(): String = "#$value"
}

interface ConstantPoolEntry {
    val occupiesTwoSlots: Boolean
        get() = false
}

sealed interface ConstantPoolSlot {
    data class Entry(val value: ConstantPoolEntry) : ConstantPoolSlot
    data object Unusable : ConstantPoolSlot
}

class ConstantPool private constructor(
    private val slots: List<ConstantPoolSlot>,
) {
    val slotCount: Int
        get() = slots.size

    val constantPoolCount: Int
        get() = slotCount + 1

    operator fun get(index: ConstantPoolIndex): ConstantPoolEntry =
        when (val slot = slotAt(index)) {
            is ConstantPoolSlot.Entry -> slot.value
            ConstantPoolSlot.Unusable -> throw ConstantPoolFormatException(
                "Constant pool index $index points to an unusable two-slot placeholder",
            )
        }

    fun slotAt(index: ConstantPoolIndex): ConstantPoolSlot {
        if (index.value >= constantPoolCount) {
            throw ConstantPoolFormatException(
                "Constant pool index $index is outside constant_pool_count=$constantPoolCount",
            )
        }
        return slots[index.value - 1]
    }

    fun validateReferences() {
        val referencedNameAndTypes = referencedNameAndTypeIndices()
        slots.forEachIndexed { slotIndex, slot ->
            if (slot is ConstantPoolSlot.Entry) {
                validateEntry(ConstantPoolIndex(slotIndex + 1), slot.value, referencedNameAndTypes)
            }
        }
    }

    private fun referencedNameAndTypeIndices(): Set<ConstantPoolIndex> =
        slots.mapNotNull { slot ->
            val entry = (slot as? ConstantPoolSlot.Entry)?.value
            when (entry) {
                is ConstantMemberRefEntry -> entry.nameAndTypeIndex
                is ConstantDynamicEntry -> entry.nameAndTypeIndex
                is ConstantInvokeDynamicEntry -> entry.nameAndTypeIndex
                else -> null
            }
        }.toSet()

    private fun validateEntry(
        owner: ConstantPoolIndex,
        entry: ConstantPoolEntry,
        referencedNameAndTypes: Set<ConstantPoolIndex>,
    ) {
        when (entry) {
            is ConstantUtf8Entry,
            is ConstantIntegerEntry,
            is ConstantFloatEntry,
            is ConstantLongEntry,
            is ConstantDoubleEntry,
            -> Unit

            is ConstantClassEntry -> {
                val name = expect<ConstantUtf8Entry>(owner, "name_index", entry.nameIndex)
                ClassNameValidator.validateConstantClassName(owner, "name_index", name.value)
            }
            is ConstantStringEntry -> expect<ConstantUtf8Entry>(owner, "string_index", entry.stringIndex)
            is ConstantNameAndTypeEntry -> {
                val name = expect<ConstantUtf8Entry>(owner, "name_index", entry.nameIndex)
                val descriptor = expect<ConstantUtf8Entry>(owner, "descriptor_index", entry.descriptorIndex)
                ClassNameValidator.validateUnqualifiedName(owner, "name_index", name.value)
                if (owner !in referencedNameAndTypes) {
                    validateNameAndTypeDescriptor(owner, descriptor.value)
                }
            }

            is ConstantFieldRefEntry -> {
                validateMemberRef(owner, entry)
                validateFieldDescriptor(owner, "name_and_type_index", entry.nameAndTypeIndex)
            }
            is ConstantMethodRefEntry -> {
                validateMemberRef(owner, entry)
                validateMethodDescriptor(owner, "name_and_type_index", entry.nameAndTypeIndex, allowInit = true)
            }
            is ConstantInterfaceMethodRefEntry -> {
                validateMemberRef(owner, entry)
                validateMethodDescriptor(owner, "name_and_type_index", entry.nameAndTypeIndex, allowInit = false)
            }
            is ConstantMethodTypeEntry -> {
                val descriptor = expect<ConstantUtf8Entry>(owner, "descriptor_index", entry.descriptorIndex)
                DescriptorValidator.validateMethodDescriptor(owner, "descriptor_index", descriptor.value)
            }
            is ConstantMethodHandleEntry -> validateMethodHandle(owner, entry)
            is ConstantDynamicEntry -> {
                expect<ConstantNameAndTypeEntry>(owner, "name_and_type_index", entry.nameAndTypeIndex)
                validateDynamicConstantName(owner, entry.nameAndTypeIndex)
                validateFieldDescriptor(owner, "name_and_type_index", entry.nameAndTypeIndex)
            }
            is ConstantInvokeDynamicEntry -> {
                expect<ConstantNameAndTypeEntry>(owner, "name_and_type_index", entry.nameAndTypeIndex)
                validateMethodDescriptor(owner, "name_and_type_index", entry.nameAndTypeIndex, allowInit = false)
            }
            is ConstantModuleEntry -> {
                val name = expect<ConstantUtf8Entry>(owner, "name_index", entry.nameIndex)
                ModulePackageNameValidator.validateModuleName(owner, "name_index", name.value)
            }

            is ConstantPackageEntry -> {
                val name = expect<ConstantUtf8Entry>(owner, "name_index", entry.nameIndex)
                ModulePackageNameValidator.validatePackageName(owner, "name_index", name.value)
            }
        }
    }

    private fun validateDynamicConstantName(owner: ConstantPoolIndex, nameAndTypeIndex: ConstantPoolIndex) {
        val nameAndType = expect<ConstantNameAndTypeEntry>(owner, "name_and_type_index", nameAndTypeIndex)
        val name = expect<ConstantUtf8Entry>(nameAndTypeIndex, "name_index", nameAndType.nameIndex)
        if (name.value == "<init>" || name.value == "<clinit>") {
            throw ClassFileFormatException(
                "Invalid constant pool reference from $owner name_index: " +
                    "dynamic constant name ${name.value} is not permitted",
            )
        }
    }

    private fun validateNameAndTypeDescriptor(owner: ConstantPoolIndex, descriptor: String) {
        if (descriptor.startsWith("(")) {
            DescriptorValidator.validateMethodDescriptor(owner, "descriptor_index", descriptor)
        } else {
            DescriptorValidator.validateFieldDescriptor(owner, "descriptor_index", descriptor)
        }
    }

    private fun validateMemberRef(owner: ConstantPoolIndex, entry: ConstantMemberRefEntry) {
        val ownerClass = expect<ConstantClassEntry>(owner, "class_index", entry.classIndex)
        val ownerName = expect<ConstantUtf8Entry>(owner, "class_index name_index", ownerClass.nameIndex)
        ClassNameValidator.validateInternalBinaryName(owner, "class_index", ownerName.value)
        expect<ConstantNameAndTypeEntry>(owner, "name_and_type_index", entry.nameAndTypeIndex)
    }

    private fun validateFieldDescriptor(
        owner: ConstantPoolIndex,
        role: String,
        nameAndTypeIndex: ConstantPoolIndex,
    ) {
        val nameAndType = expect<ConstantNameAndTypeEntry>(owner, role, nameAndTypeIndex)
        val descriptor = expect<ConstantUtf8Entry>(nameAndTypeIndex, "descriptor_index", nameAndType.descriptorIndex)
        DescriptorValidator.validateFieldDescriptor(owner, "descriptor_index", descriptor.value)
    }

    private fun validateMethodDescriptor(
        owner: ConstantPoolIndex,
        role: String,
        nameAndTypeIndex: ConstantPoolIndex,
        allowInit: Boolean,
    ) {
        val nameAndType = expect<ConstantNameAndTypeEntry>(owner, role, nameAndTypeIndex)
        val name = expect<ConstantUtf8Entry>(nameAndTypeIndex, "name_index", nameAndType.nameIndex)
        val descriptor = expect<ConstantUtf8Entry>(nameAndTypeIndex, "descriptor_index", nameAndType.descriptorIndex)
        ClassNameValidator.validateMethodName(owner, "name_index", name.value, allowInit)
        if (name.value == "<init>") {
            DescriptorValidator.validateMethodDescriptor(owner, "descriptor_index", descriptor.value)
            if (!descriptor.value.endsWith("V")) {
                throw ClassFileFormatException(
                    "Invalid constant pool reference from $owner descriptor_index: " +
                        "method name <init> must have a void method descriptor",
                )
            }
        } else {
            DescriptorValidator.validateMethodDescriptor(owner, "descriptor_index", descriptor.value)
        }
    }

    private fun validateMethodHandle(owner: ConstantPoolIndex, entry: ConstantMethodHandleEntry) {
        when (entry.referenceKind) {
            MethodHandleReferenceKind.GetField,
            MethodHandleReferenceKind.GetStatic,
            MethodHandleReferenceKind.PutField,
            MethodHandleReferenceKind.PutStatic,
            -> expect<ConstantFieldRefEntry>(owner, "reference_index", entry.referenceIndex)

            MethodHandleReferenceKind.InvokeVirtual -> validateOrdinaryInvocationMethodHandle(
                owner = owner,
                entry = entry,
                referencedEntry = expect<ConstantMethodRefEntry>(owner, "reference_index", entry.referenceIndex),
            )

            MethodHandleReferenceKind.NewInvokeSpecial -> validateNewInvokeSpecialMethodHandle(owner, entry)

            MethodHandleReferenceKind.InvokeStatic,
            MethodHandleReferenceKind.InvokeSpecial,
            -> validateOrdinaryInvocationMethodHandle(
                owner = owner,
                entry = entry,
                referencedEntry = expectOneOf(
                    owner = owner,
                    role = "reference_index",
                    index = entry.referenceIndex,
                    expected = "ConstantMethodRefEntry or ConstantInterfaceMethodRefEntry",
                ) { referenced ->
                    referenced is ConstantMethodRefEntry || referenced is ConstantInterfaceMethodRefEntry
                },
            )

            MethodHandleReferenceKind.InvokeInterface -> validateOrdinaryInvocationMethodHandle(
                owner = owner,
                entry = entry,
                referencedEntry = expect<ConstantInterfaceMethodRefEntry>(owner, "reference_index", entry.referenceIndex),
            )
        }
    }

    private fun validateNewInvokeSpecialMethodHandle(owner: ConstantPoolIndex, entry: ConstantMethodHandleEntry) {
        val methodRef = expect<ConstantMethodRefEntry>(owner, "reference_index", entry.referenceIndex)
        val name = methodHandleTargetName(owner, entry.referenceKind, entry.referenceIndex, methodRef)
        if (name.value != "<init>") {
            throw ClassFileFormatException(
                "Invalid constant pool reference from $owner reference_index to ${entry.referenceIndex}: " +
                    "reference_kind NewInvokeSpecial must target <init> but found ${name.value}",
            )
        }
    }

    private fun validateOrdinaryInvocationMethodHandle(
        owner: ConstantPoolIndex,
        entry: ConstantMethodHandleEntry,
        referencedEntry: ConstantPoolEntry,
    ) {
        val name = methodHandleTargetName(owner, entry.referenceKind, entry.referenceIndex, referencedEntry)
        if (name.value == "<init>" || name.value == "<clinit>") {
            throw ClassFileFormatException(
                "Invalid constant pool reference from $owner reference_index to ${entry.referenceIndex}: " +
                    "reference_kind ${entry.referenceKind} must not target ${name.value}",
            )
        }
    }

    private fun methodHandleTargetName(
        owner: ConstantPoolIndex,
        referenceKind: MethodHandleReferenceKind,
        referenceIndex: ConstantPoolIndex,
        referencedEntry: ConstantPoolEntry,
    ): ConstantUtf8Entry {
        val nameAndTypeIndex = when (referencedEntry) {
            is ConstantMethodRefEntry -> referencedEntry.nameAndTypeIndex
            is ConstantInterfaceMethodRefEntry -> referencedEntry.nameAndTypeIndex
            else -> throw ClassFileFormatException(
                "Invalid constant pool reference from $owner reference_index to $referenceIndex: " +
                    "reference_kind $referenceKind expected a method reference but found " +
                    referencedEntry.javaClass.simpleName,
            )
        }
        val nameAndType = expect<ConstantNameAndTypeEntry>(
            owner = owner,
            role = "reference_index name_and_type_index",
            index = nameAndTypeIndex,
        )
        return expect<ConstantUtf8Entry>(
            owner = nameAndTypeIndex,
            role = "name_index",
            index = nameAndType.nameIndex,
        )
    }

    private inline fun <reified T : ConstantPoolEntry> expect(
        owner: ConstantPoolIndex,
        role: String,
        index: ConstantPoolIndex,
    ): T = expectOneOf(owner, role, index, T::class.java.simpleName) { referenced -> referenced is T } as T

    private fun expectOneOf(
        owner: ConstantPoolIndex,
        role: String,
        index: ConstantPoolIndex,
        expected: String,
        matches: (ConstantPoolEntry) -> Boolean,
    ): ConstantPoolEntry =
        when (val slot = referenceSlot(owner, role, index)) {
            is ConstantPoolSlot.Entry -> {
                if (!matches(slot.value)) {
                    throw ClassFileFormatException(
                        "Invalid constant pool reference from $owner $role to $index: " +
                            "expected $expected but found ${slot.value.javaClass.simpleName}",
                    )
                }
                slot.value
            }

            ConstantPoolSlot.Unusable -> throw ClassFileFormatException(
                "Invalid constant pool reference from $owner $role to $index: target is an unusable two-slot placeholder",
            )
        }

    private fun referenceSlot(
        owner: ConstantPoolIndex,
        role: String,
        index: ConstantPoolIndex,
    ): ConstantPoolSlot = try {
        slotAt(index)
    } catch (exception: ConstantPoolFormatException) {
        throw ClassFileFormatException(
            "Invalid constant pool reference from $owner $role to $index: ${exception.message}",
        )
    }

    companion object {
        fun fromEntries(entries: List<ConstantPoolEntry>): ConstantPool {
            val slots = buildList {
                entries.forEach { entry ->
                    add(ConstantPoolSlot.Entry(entry))
                    if (entry.occupiesTwoSlots) {
                        add(ConstantPoolSlot.Unusable)
                    }
                }
            }
            return ConstantPool(slots)
        }
    }
}
