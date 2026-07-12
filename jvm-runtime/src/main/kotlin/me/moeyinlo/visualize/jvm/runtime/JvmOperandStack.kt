package me.moeyinlo.visualize.jvm.runtime

class JvmOperandStack(maxStack: Int) {
    init {
        require(maxStack >= 0) { "max_stack must be non-negative: $maxStack" }
    }

    private val values = mutableListOf<JvmValue>()
    private val maxSlotDepth = maxStack

    var slotDepth: Int = 0
        private set

    val valueCount: Int
        get() = values.size

    fun push(value: JvmValue) {
        val nextDepth = slotDepth + value.category.slotWidth
        if (nextDepth > maxSlotDepth) {
            throw JvmOperandStackOverflowException(
                "Operand stack depth $nextDepth exceeds max_stack=$maxSlotDepth",
            )
        }

        values.add(value)
        slotDepth = nextDepth
    }

    fun pop(): JvmValue {
        val value = values.removeLastOrNull()
            ?: throw JvmOperandStackUnderflowException("Operand stack is empty")
        slotDepth -= value.category.slotWidth
        return value
    }

    fun peek(): JvmValue =
        values.lastOrNull()
            ?: throw JvmOperandStackUnderflowException("Operand stack is empty")

    fun toList(): List<JvmValue> = values.toList()
}

class JvmOperandStackOverflowException(message: String) : IllegalStateException(message)

class JvmOperandStackUnderflowException(message: String) : IllegalStateException(message)
