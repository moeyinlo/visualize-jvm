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
