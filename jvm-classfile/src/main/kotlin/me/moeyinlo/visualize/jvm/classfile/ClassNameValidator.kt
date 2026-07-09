package me.moeyinlo.visualize.jvm.classfile

object ClassNameValidator {
    fun validateConstantClassName(
        owner: ConstantPoolIndex,
        role: String,
        value: String,
    ) {
        if (value.startsWith("[")) {
            DescriptorValidator.validateFieldDescriptor(owner, role, value)
            return
        }
        validateInternalBinaryName(owner, role, value)
    }

    fun validateInternalBinaryName(
        owner: ConstantPoolIndex,
        role: String,
        value: String,
    ) {
        if (value.isEmpty()) {
            fail(owner, role, value, "must not be empty")
        }

        value.split('/').forEachIndexed { index, segment ->
            if (segment.isEmpty()) {
                fail(owner, role, value, "contains an empty unqualified name segment at position $index")
            }
            val forbidden = segment.firstOrNull { it == '.' || it == ';' || it == '[' || it == '/' }
            if (forbidden != null) {
                fail(
                    owner = owner,
                    role = role,
                    value = value,
                    reason = "segment '$segment' contains forbidden character '$forbidden'",
                )
            }
        }
    }

    fun validateUnqualifiedName(
        owner: ConstantPoolIndex,
        role: String,
        value: String,
    ) {
        if (value.isEmpty()) {
            failUnqualified(owner, role, value, "must not be empty")
        }
        val forbidden = value.firstOrNull { it == '.' || it == ';' || it == '[' || it == '/' }
        if (forbidden != null) {
            failUnqualified(owner, role, value, "contains forbidden character '$forbidden'")
        }
    }

    fun validateMethodName(
        owner: ConstantPoolIndex,
        role: String,
        value: String,
        allowInit: Boolean,
    ) {
        validateUnqualifiedName(owner, role, value)
        when (value) {
            "<init>" -> {
                if (!allowInit) {
                    failMethodName(owner, role, value, "special name <init> is not permitted here")
                }
                return
            }

            "<clinit>" -> failMethodName(owner, role, value, "special name <clinit> is not permitted here")
        }

        val forbidden = value.firstOrNull { it == '<' || it == '>' }
        if (forbidden != null) {
            failMethodName(owner, role, value, "contains forbidden character '$forbidden'")
        }
    }

    private fun fail(
        owner: ConstantPoolIndex,
        role: String,
        value: String,
        reason: String,
    ): Nothing =
        throw ClassFileFormatException(
            "Invalid constant pool reference from $owner $role: " +
                "'$value' is not a binary name in internal form: $reason",
        )

    private fun failUnqualified(
        owner: ConstantPoolIndex,
        role: String,
        value: String,
        reason: String,
    ): Nothing =
        throw ClassFileFormatException(
            "Invalid constant pool reference from $owner $role: " +
                "'$value' is not a valid unqualified name: $reason",
        )

    private fun failMethodName(
        owner: ConstantPoolIndex,
        role: String,
        value: String,
        reason: String,
    ): Nothing =
        throw ClassFileFormatException(
            "Invalid constant pool reference from $owner $role: " +
                "'$value' is not a valid method name: $reason",
        )
}
