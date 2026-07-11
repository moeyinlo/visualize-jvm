package me.moeyinlo.visualize.jvm.verifier

object MethodDescriptorVerificationTypeParser {
    fun parseParameterTypes(descriptor: String): List<VerificationType> {
        val parser = Parser(descriptor)
        return parser.parseParameterTypes()
    }

    private class Parser(
        private val descriptor: String,
    ) {
        private var position: Int = 0

        fun parseParameterTypes(): List<VerificationType> {
            if (peek() != '(') {
                fail("must start with '('")
            }
            position++
            val parameters = mutableListOf<VerificationType>()
            while (peek() != ')') {
                if (peek() == null) {
                    fail("missing ')' after parameter descriptors")
                }
                parameters += parsePrimitiveFieldType()
            }
            position++
            parseReturnDescriptor()
            if (peek() != null) {
                fail("has trailing characters at offset $position")
            }
            return parameters
        }

        private fun parseReturnDescriptor() {
            if (peek() == 'V') {
                position++
                return
            }
            parsePrimitiveFieldType()
        }

        private fun parsePrimitiveFieldType(): VerificationType =
            when (peek()) {
                'B',
                'C',
                'I',
                'S',
                'Z',
                -> {
                    position++
                    VerificationType.Integer
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
                null -> fail("expected field type at offset $position")
                else -> fail("unsupported field type '${peek()}' at offset $position")
            }

        private fun peek(): Char? =
            if (position < descriptor.length) {
                descriptor[position]
            } else {
                null
            }

        private fun fail(reason: String): Nothing =
            throw MethodVerificationException("Invalid method descriptor '$descriptor': $reason")
    }
}
