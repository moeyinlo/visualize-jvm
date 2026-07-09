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
        slots.forEachIndexed { slotIndex, slot ->
            if (slot is ConstantPoolSlot.Entry) {
                validateEntry(ConstantPoolIndex(slotIndex + 1), slot.value)
            }
        }
    }

    private fun validateEntry(owner: ConstantPoolIndex, entry: ConstantPoolEntry) {
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
                ClassNameValidator.validateUnqualifiedName(owner, "name_index", name.value)
                expect<ConstantUtf8Entry>(owner, "descriptor_index", entry.descriptorIndex)
            }

            is ConstantFieldRefEntry -> {
                validateMemberRef(owner, entry)
                validateFieldDescriptor(owner, "name_and_type_index", entry.nameAndTypeIndex)
            }
            is ConstantMethodRefEntry -> {
                validateMemberRef(owner, entry)
                validateMethodDescriptor(owner, "name_and_type_index", entry.nameAndTypeIndex)
            }
            is ConstantInterfaceMethodRefEntry -> {
                validateMemberRef(owner, entry)
                validateMethodDescriptor(owner, "name_and_type_index", entry.nameAndTypeIndex)
            }
            is ConstantMethodTypeEntry -> {
                val descriptor = expect<ConstantUtf8Entry>(owner, "descriptor_index", entry.descriptorIndex)
                DescriptorValidator.validateMethodDescriptor(owner, "descriptor_index", descriptor.value)
            }
            is ConstantMethodHandleEntry -> validateMethodHandle(owner, entry)
            is ConstantDynamicEntry -> {
                expect<ConstantNameAndTypeEntry>(owner, "name_and_type_index", entry.nameAndTypeIndex)
                validateFieldDescriptor(owner, "name_and_type_index", entry.nameAndTypeIndex)
            }
            is ConstantInvokeDynamicEntry -> {
                expect<ConstantNameAndTypeEntry>(owner, "name_and_type_index", entry.nameAndTypeIndex)
                validateMethodDescriptor(owner, "name_and_type_index", entry.nameAndTypeIndex)
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

    private fun validateMemberRef(owner: ConstantPoolIndex, entry: ConstantMemberRefEntry) {
        expect<ConstantClassEntry>(owner, "class_index", entry.classIndex)
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
    ) {
        val nameAndType = expect<ConstantNameAndTypeEntry>(owner, role, nameAndTypeIndex)
        val descriptor = expect<ConstantUtf8Entry>(nameAndTypeIndex, "descriptor_index", nameAndType.descriptorIndex)
        DescriptorValidator.validateMethodDescriptor(owner, "descriptor_index", descriptor.value)
    }

    private fun validateMethodHandle(owner: ConstantPoolIndex, entry: ConstantMethodHandleEntry) {
        when (entry.referenceKind) {
            MethodHandleReferenceKind.GetField,
            MethodHandleReferenceKind.GetStatic,
            MethodHandleReferenceKind.PutField,
            MethodHandleReferenceKind.PutStatic,
            -> expect<ConstantFieldRefEntry>(owner, "reference_index", entry.referenceIndex)

            MethodHandleReferenceKind.InvokeVirtual,
            MethodHandleReferenceKind.NewInvokeSpecial,
            -> expect<ConstantMethodRefEntry>(owner, "reference_index", entry.referenceIndex)

            MethodHandleReferenceKind.InvokeStatic,
            MethodHandleReferenceKind.InvokeSpecial,
            -> expectOneOf(
                owner = owner,
                role = "reference_index",
                index = entry.referenceIndex,
                expected = "ConstantMethodRefEntry or ConstantInterfaceMethodRefEntry",
            ) { referenced ->
                referenced is ConstantMethodRefEntry || referenced is ConstantInterfaceMethodRefEntry
            }

            MethodHandleReferenceKind.InvokeInterface ->
                expect<ConstantInterfaceMethodRefEntry>(owner, "reference_index", entry.referenceIndex)
        }
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
