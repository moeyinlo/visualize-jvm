package me.moeyinlo.visualize.jvm.verifier

data class MethodDescriptorVerificationTypes(
    val parameterTypes: List<VerificationType>,
    val returnType: VerificationType?,
)

object MethodDescriptorVerificationTypeParser {
    fun parse(descriptor: String): MethodDescriptorVerificationTypes {
        val parser = Parser(descriptor, descriptorKind = "method")
        return parser.parse()
    }

    fun parseParameterTypes(descriptor: String): List<VerificationType> {
        return parse(descriptor).parameterTypes
    }

    fun parseFieldType(descriptor: String): VerificationType {
        return Parser(descriptor, descriptorKind = "field").parseFieldDescriptor()
    }

    private class Parser(
        private val descriptor: String,
        private val descriptorKind: String,
    ) {
        private var position: Int = 0

        fun parseFieldDescriptor(): VerificationType {
            val type = parseFieldType()
            if (peek() != null) {
                fail("has trailing characters at offset $position")
            }
            return type
        }

        fun parse(): MethodDescriptorVerificationTypes {
            if (peek() != '(') {
                fail("must start with '('")
            }
            position++
            val parameters = mutableListOf<VerificationType>()
            while (peek() != ')') {
                if (peek() == null) {
                    fail("missing ')' after parameter descriptors")
                }
                parameters += parseFieldType()
            }
            position++
            val returnType = parseReturnDescriptor()
            if (peek() != null) {
                fail("has trailing characters at offset $position")
            }
            return MethodDescriptorVerificationTypes(
                parameterTypes = parameters,
                returnType = returnType,
            )
        }

        private fun parseReturnDescriptor(): VerificationType? {
            if (peek() == 'V') {
                position++
                return null
            }
            return parseFieldType()
        }

        private fun parseFieldType(preserveSmallPrimitive: Boolean = false): VerificationType =
            when (peek()) {
                'B' -> {
                    position++
                    if (preserveSmallPrimitive) VerificationType.Byte else VerificationType.Integer
                }
                'C' -> {
                    position++
                    if (preserveSmallPrimitive) VerificationType.Char else VerificationType.Integer
                }
                'I' -> {
                    position++
                    VerificationType.Integer
                }
                'S' -> {
                    position++
                    if (preserveSmallPrimitive) VerificationType.Short else VerificationType.Integer
                }
                'Z' -> {
                    position++
                    if (preserveSmallPrimitive) VerificationType.Boolean else VerificationType.Integer
                }
                'F' -> {
                    position++
                    VerificationType.Float
                }
                'J' -> {
                    position++
                    VerificationType.Long
                }
                'D' -> {
                    position++
                    VerificationType.Double
                }
                'L' -> parseClassType()
                '[' -> parseArrayType()
                null -> fail("expected field type at offset $position")
                else -> fail("unsupported field type '${peek()}' at offset $position")
            }

        private fun parseClassType(): VerificationType.ClassType {
            val start = position
            position++
            val nameStart = position
            while (peek() != ';') {
                if (peek() == null) {
                    fail("missing ';' after class type at offset $start")
                }
                position++
            }
            if (position == nameStart) {
                fail("empty class name at offset $start")
            }
            val internalName = descriptor.substring(nameStart, position)
            position++
            return VerificationType.ClassType(internalName)
        }

        private fun parseArrayType(): VerificationType.ArrayOf {
            position++
            return VerificationType.ArrayOf(parseFieldType(preserveSmallPrimitive = true))
        }

        private fun peek(): Char? =
            if (position < descriptor.length) {
                descriptor[position]
            } else {
                null
            }

        private fun fail(reason: String): Nothing =
            throw MethodVerificationException("Invalid $descriptorKind descriptor '$descriptor': $reason")
    }
}
