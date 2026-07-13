package me.moeyinlo.visualize.jvm.runtime

class JvmLocalVariables(maxLocals: Int) {
    init {
        require(maxLocals >= 0) { "max_locals must be non-negative: $maxLocals" }
    }

    private val slots = MutableList<LocalVariableSlot>(maxLocals) { LocalVariableSlot.Empty }

    val size: Int
        get() = slots.size

    fun load(index: Int): JvmValue {
        checkIndex(index = index, width = 1)
        return when (val slot = slots[index]) {
            LocalVariableSlot.Empty -> throw JvmLocalVariablesInvalidSlotException(
                "Local variable $index has no valid value",
            )
            is LocalVariableSlot.HighWord -> throw JvmLocalVariablesInvalidSlotException(
                "Local variable $index is the high word of a category-2 value stored at ${slot.lowerIndex}",
            )
            is LocalVariableSlot.Value -> slot.value
        }
    }

    fun store(index: Int, value: JvmValue) {
        val width = value.category.slotWidth
        checkIndex(index = index, width = width)
        clearOverlappingSlots(index = index, width = width)

        slots[index] = LocalVariableSlot.Value(value)
        if (width == 2) {
            slots[index + 1] = LocalVariableSlot.HighWord(lowerIndex = index)
        }
    }

    private fun clearOverlappingSlots(index: Int, width: Int) {
        for (slotIndex in index until index + width) {
            clearSlot(slotIndex)
        }
    }

    private fun clearSlot(index: Int) {
        when (val slot = slots[index]) {
            LocalVariableSlot.Empty -> Unit
            is LocalVariableSlot.HighWord -> {
                slots[slot.lowerIndex] = LocalVariableSlot.Empty
                slots[index] = LocalVariableSlot.Empty
            }
            is LocalVariableSlot.Value -> {
                slots[index] = LocalVariableSlot.Empty
                if (slot.value.category == JvmValueCategory.Category2) {
                    slots[index + 1] = LocalVariableSlot.Empty
                }
            }
        }
    }

    private fun checkIndex(index: Int, width: Int) {
        if (index < 0 || index + width > slots.size) {
            throw JvmLocalVariablesIndexException(
                "Local variable index $index with width $width exceeds max_locals=${slots.size}",
            )
        }
    }
}

private sealed interface LocalVariableSlot {
    data object Empty : LocalVariableSlot
    data class Value(val value: JvmValue) : LocalVariableSlot
    data class HighWord(val lowerIndex: Int) : LocalVariableSlot
}

class JvmLocalVariablesIndexException(message: String) : IllegalStateException(message)

class JvmLocalVariablesInvalidSlotException(message: String) : IllegalStateException(message)
