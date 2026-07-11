package me.moeyinlo.visualize.jvm.verifier

class VerifierOperandStack private constructor(
    val values: List<VerificationType>,
    private val maxStack: Int,
) {
    val depth: Int
        get() = values.stackDepth()

    fun push(value: VerificationType): VerifierOperandStack {
        val nextValues = values + value
        validateDepth(nextValues, maxStack)
        return VerifierOperandStack(nextValues, maxStack)
    }

    fun pop(expected: VerificationType): VerifierOperandStackPop {
        val actual = values.lastOrNull()
            ?: throw MethodVerificationException("Operand stack is empty, expected $expected")
        if (!actual.isAssignableTo(expected)) {
            throw MethodVerificationException(
                "Operand stack top contains $actual, expected $expected",
            )
        }
        return VerifierOperandStackPop(
            value = actual,
            stack = VerifierOperandStack(values.dropLast(1), maxStack),
        )
    }

    companion object {
        fun empty(maxStack: Int): VerifierOperandStack =
            VerifierOperandStack(values = emptyList(), maxStack = maxStack)

        fun fromFrame(
            stack: List<VerificationType>,
            maxStack: Int,
        ): VerifierOperandStack {
            validateDepth(stack, maxStack)
            return VerifierOperandStack(values = stack.toList(), maxStack = maxStack)
        }

        private fun validateDepth(values: List<VerificationType>, maxStack: Int) {
            val depth = values.stackDepth()
            if (depth > maxStack) {
                throw MethodVerificationException(
                    "Operand stack depth $depth exceeds max_stack=$maxStack",
                )
            }
        }

        private fun List<VerificationType>.stackDepth(): Int =
            sumOf { type -> type.locationCount }
    }
}

data class VerifierOperandStackPop(
    val value: VerificationType,
    val stack: VerifierOperandStack,
)
