package me.moeyinlo.visualize.jvm.verifier

class VerifierLocalVariables private constructor(
    val slots: List<VerificationType>,
) {
    fun load(index: Int, expected: VerificationType): VerificationType {
        checkIndex(index = index, width = expected.locationCount)
        val actual = slots[index]
        if (!actual.isAssignableTo(expected)) {
            throw MethodVerificationException(
                "Local variable $index contains $actual, expected $expected",
            )
        }
        return actual
    }

    fun store(index: Int, value: VerificationType): VerifierLocalVariables {
        checkIndex(index = index, width = value.locationCount)
        val updated = slots.toMutableList()

        if (index > 0 && updated[index] == VerificationType.Top && updated[index - 1].locationCount == 2) {
            updated[index - 1] = VerificationType.Top
        }

        updated[index] = value
        if (value.locationCount == 2) {
            updated[index + 1] = VerificationType.Top
        }

        return VerifierLocalVariables(updated.toList())
    }

    private fun checkIndex(index: Int, width: Int) {
        if (index < 0 || index + width > slots.size) {
            throw MethodVerificationException(
                "Local variable index $index with width $width exceeds max_locals=${slots.size}",
            )
        }
    }

    companion object {
        fun fromCompact(
            locals: List<VerificationType>,
            maxLocals: Int,
        ): VerifierLocalVariables {
            val slots = mutableListOf<VerificationType>()
            locals.forEach { local ->
                val nextSize = slots.size + local.locationCount
                if (nextSize > maxLocals) {
                    throw MethodVerificationException(
                        "Compact locals use $nextSize local variable unit(s), exceeding max_locals=$maxLocals",
                    )
                }
                slots += local
                repeat(local.locationCount - 1) {
                    slots += VerificationType.Top
                }
            }

            while (slots.size < maxLocals) {
                slots += VerificationType.Top
            }

            return VerifierLocalVariables(slots.toList())
        }
    }
}
