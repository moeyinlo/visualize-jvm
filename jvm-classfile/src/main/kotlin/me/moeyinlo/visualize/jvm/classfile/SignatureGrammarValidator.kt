package me.moeyinlo.visualize.jvm.classfile

object SignatureGrammarValidator {
    fun validateForOwner(
        ownerPath: String,
        signature: String,
    ) {
        if (ownerPath.startsWith("fields[") || ownerPath.contains(".components[")) {
            validateFieldSignature(ownerPath, signature)
        } else if (ownerPath.startsWith("methods[")) {
            validateMethodSignature(ownerPath, signature)
        } else if (ownerPath.startsWith("ClassFile")) {
            validateClassSignature(ownerPath, signature)
        }
    }

    fun validateFieldSignature(
        ownerPath: String,
        signature: String,
    ) {
        val parser = SignatureParser(ownerPath, signature, "field signature")
        parser.parseReferenceTypeSignature()
        parser.expectEnd()
    }

    fun validateClassSignature(
        ownerPath: String,
        signature: String,
    ) {
        val parser = SignatureParser(ownerPath, signature, "class signature")
        parser.parseClassSignature()
        parser.expectEnd()
    }

    fun validateMethodSignature(
        ownerPath: String,
        signature: String,
    ) {
        val parser = SignatureParser(ownerPath, signature, "method signature")
        parser.parseMethodSignature()
        parser.expectEnd()
    }

    private class SignatureParser(
        private val ownerPath: String,
        private val signature: String,
        private val signatureKind: String,
    ) {
        var position: Int = 0
            private set

        fun parseReferenceTypeSignature() {
            when (peek()) {
                'L' -> parseClassTypeSignature()
                'T' -> parseTypeVariableSignature()
                '[' -> parseArrayTypeSignature()
                else -> fail("expected ReferenceTypeSignature at offset $position")
            }
        }

        fun parseClassSignature() {
            if (peek() == '<') {
                parseTypeParameters()
            }
            parseClassTypeSignature()
            while (peek() != null) {
                parseClassTypeSignature()
            }
        }

        fun parseMethodSignature() {
            if (peek() == '<') {
                parseTypeParameters()
            }
            expect('(')
            while (peek() != ')') {
                if (peek() == null) {
                    fail("missing ')' after method parameters")
                }
                parseJavaTypeSignature()
            }
            expect(')')
            if (peek() == 'V') {
                position++
            } else {
                parseJavaTypeSignature()
            }
            while (peek() == '^') {
                position++
                when (peek()) {
                    'L' -> parseClassTypeSignature()
                    'T' -> parseTypeVariableSignature()
                    else -> fail("expected ThrowsSignature at offset $position")
                }
            }
        }

        fun expectEnd() {
            if (position != signature.length) {
                fail("has trailing characters at offset $position")
            }
        }

        private fun parseJavaTypeSignature() {
            when (peek()) {
                'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> position++
                else -> parseReferenceTypeSignature()
            }
        }

        private fun parseClassTypeSignature() {
            expect('L')
            parseIdentifier()
            while (peek() == '/') {
                position++
                parseIdentifier()
            }
            if (peek() == '<') {
                parseTypeArguments()
            }
            while (peek() == '.') {
                position++
                parseSimpleClassTypeSignature()
            }
            expect(';')
        }

        private fun parseSimpleClassTypeSignature() {
            parseIdentifier()
            if (peek() == '<') {
                parseTypeArguments()
            }
        }

        private fun parseTypeArguments() {
            expect('<')
            var count = 0
            while (peek() != '>') {
                if (peek() == null) {
                    fail("missing '>' for type arguments")
                }
                parseTypeArgument()
                count++
            }
            if (count == 0) {
                fail("type arguments must not be empty")
            }
            expect('>')
        }

        private fun parseTypeArgument() {
            if (peek() == '*') {
                position++
                return
            }
            if (peek() == '+' || peek() == '-') {
                position++
            }
            parseReferenceTypeSignature()
        }

        private fun parseTypeVariableSignature() {
            expect('T')
            parseIdentifier()
            expect(';')
        }

        private fun parseArrayTypeSignature() {
            expect('[')
            parseJavaTypeSignature()
        }

        private fun parseTypeParameters() {
            expect('<')
            var count = 0
            while (peek() != '>') {
                if (peek() == null) {
                    fail("missing '>' for type parameters")
                }
                parseTypeParameter()
                count++
            }
            if (count == 0) {
                fail("type parameters must not be empty")
            }
            expect('>')
        }

        private fun parseTypeParameter() {
            parseIdentifier()
            parseClassBound()
            while (peek() == ':') {
                parseInterfaceBound()
            }
        }

        private fun parseClassBound() {
            expect(':')
            if (peek() != ':' && peek() != '>') {
                parseReferenceTypeSignature()
            }
        }

        private fun parseInterfaceBound() {
            expect(':')
            parseReferenceTypeSignature()
        }

        private fun parseIdentifier(): String {
            val start = position
            while (true) {
                val current = peek()
                if (current == null || current in charArrayOf('.', ';', '[', '/', '<', '>', ':')) {
                    break
                }
                position++
            }
            if (position == start) {
                fail("expected Identifier at offset $start")
            }
            return signature.substring(start, position)
        }

        private fun expect(expected: Char) {
            if (peek() != expected) {
                fail("expected '$expected' at offset $position")
            }
            position++
        }

        private fun peek(): Char? =
            if (position < signature.length) {
                signature[position]
            } else {
                null
            }

        private fun fail(reason: String): Nothing =
            throw ClassFileFormatException(
                "Invalid Signature attribute at $ownerPath: " +
                    "'$signature' is not a valid $signatureKind: $reason",
            )
    }
}
