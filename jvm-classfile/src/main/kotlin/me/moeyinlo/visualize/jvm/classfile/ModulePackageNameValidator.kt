package me.moeyinlo.visualize.jvm.classfile

object ModulePackageNameValidator {
    fun validateModuleName(
        owner: ConstantPoolIndex,
        role: String,
        value: String,
    ) {
        if (value.isEmpty()) {
            fail(owner, role, value, "must not be empty")
        }
        var index = 0
        while (index < value.length) {
            val current = value[index]
            when {
                current.code in 0x0000..0x001F ->
                    fail(owner, role, value, "contains control character U+${current.code.toString(16).padStart(4, '0')}")

                current == '\\' -> {
                    val next = value.getOrNull(index + 1)
                        ?: fail(owner, role, value, "ends with an incomplete escape")
                    if (next != '\\' && next != ':' && next != '@') {
                        fail(owner, role, value, "contains unsupported escape '\\$next'")
                    }
                    index++
                }

                current == ':' || current == '@' ->
                    fail(owner, role, value, "contains unescaped reserved character '$current'")
            }
            index++
        }
    }

    fun validatePackageName(
        owner: ConstantPoolIndex,
        role: String,
        value: String,
    ) {
        ClassNameValidator.validateInternalBinaryName(owner, role, value)
    }

    private fun fail(
        owner: ConstantPoolIndex,
        role: String,
        value: String,
        reason: String,
    ): Nothing =
        throw ClassFileFormatException(
            "Invalid constant pool reference from $owner $role: " +
                "'$value' is not a valid module name: $reason",
        )
}
