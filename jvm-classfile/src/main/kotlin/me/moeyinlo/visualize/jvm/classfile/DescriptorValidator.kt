package me.moeyinlo.visualize.jvm.classfile

object DescriptorValidator {
    fun validateFieldDescriptor(
        owner: ConstantPoolIndex,
        role: String,
        descriptor: String,
    ) {
        val parser = FieldDescriptorParser(owner, role, descriptor)
        parser.parseFieldType()
        if (!parser.isAtEnd()) {
            parser.fail("has trailing characters at offset ${parser.position}")
        }
    }

    private class FieldDescriptorParser(
        private val owner: ConstantPoolIndex,
        private val role: String,
        private val descriptor: String,
    ) {
        var position: Int = 0
            private set

        fun isAtEnd(): Boolean = position == descriptor.length

        fun parseFieldType() {
            when (peek()) {
                'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> position++
                'L' -> parseClassType()
                '[' -> parseArrayType()
                null -> fail("is empty")
                else -> fail("expected field type at offset $position")
            }
        }

        fun fail(reason: String): Nothing =
            throw ClassFileFormatException(
                "Invalid constant pool reference from $owner $role: " +
                    "'$descriptor' is not a valid field descriptor: $reason",
            )

        private fun parseClassType() {
            position++
            val start = position
            while (peek() != null && peek() != ';') {
                position++
            }
            if (peek() != ';') {
                fail("class type missing ';' terminator")
            }
            val className = descriptor.substring(start, position)
            ClassNameValidator.validateInternalBinaryName(owner, role, className)
            position++
        }

        private fun parseArrayType() {
            val start = position
            while (peek() == '[') {
                position++
            }
            val dimensions = position - start
            if (dimensions > 255) {
                fail("array type has $dimensions dimensions; maximum is 255")
            }
            parseFieldType()
        }

        private fun peek(): Char? =
            if (position < descriptor.length) {
                descriptor[position]
            } else {
                null
            }
    }
}
