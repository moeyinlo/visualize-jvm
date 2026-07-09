package me.moeyinlo.visualize.jvm.classfile

object DescriptorValidator {
    fun validateFieldDescriptor(
        owner: ConstantPoolIndex,
        role: String,
        descriptor: String,
    ) {
        val parser = DescriptorParser(owner, role, descriptor, "field")
        parser.parseFieldType()
        if (!parser.isAtEnd()) {
            parser.fail("has trailing characters at offset ${parser.position}")
        }
    }

    fun validateMethodDescriptor(
        owner: ConstantPoolIndex,
        role: String,
        descriptor: String,
    ) {
        DescriptorParser(owner, role, descriptor, "method").parseMethodDescriptor()
    }

    private class DescriptorParser(
        private val owner: ConstantPoolIndex,
        private val role: String,
        private val descriptor: String,
        private val descriptorKind: String,
    ) {
        var position: Int = 0
            private set

        fun isAtEnd(): Boolean = position == descriptor.length

        fun parseFieldType(): Int =
            when (peek()) {
                'B', 'C', 'F', 'I', 'S', 'Z' -> {
                    position++
                    1
                }

                'D', 'J' -> {
                    position++
                    2
                }

                'L' -> {
                    parseClassType()
                    1
                }

                '[' -> {
                    parseArrayType()
                    1
                }

                null -> fail("is empty")
                else -> fail("expected field type at offset $position")
            }

        fun parseMethodDescriptor() {
            if (peek() != '(') {
                fail("must start with '('")
            }
            position++
            var parameterUnits = 0
            while (peek() != ')') {
                if (peek() == null) {
                    fail("missing ')' after parameter descriptors")
                }
                parameterUnits += parseFieldType()
                if (parameterUnits > 255) {
                    fail("parameter length is $parameterUnits; maximum is 255")
                }
            }
            position++
            if (peek() == 'V') {
                position++
            } else {
                parseFieldType()
            }
            if (!isAtEnd()) {
                fail("has trailing characters at offset $position")
            }
        }

        fun fail(reason: String): Nothing =
            throw ClassFileFormatException(
                "Invalid constant pool reference from $owner $role: " +
                    "'$descriptor' is not a valid $descriptorKind descriptor: $reason",
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
