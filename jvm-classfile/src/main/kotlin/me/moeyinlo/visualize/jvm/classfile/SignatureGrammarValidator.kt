package me.moeyinlo.visualize.jvm.classfile

object SignatureGrammarValidator {
    fun validateForOwner(
        ownerPath: String,
        signature: String,
    ) {
        if (ownerPath.startsWith("fields[") || ownerPath.contains(".components[")) {
            validateFieldSignature(ownerPath, signature)
        }
    }

    fun validateFieldSignature(
        ownerPath: String,
        signature: String,
    ) {
        val parser = SignatureParser(ownerPath, signature)
        parser.parseReferenceTypeSignature()
        parser.expectEnd("field signature")
    }

    private class SignatureParser(
        private val ownerPath: String,
        private val signature: String,
    ) {
        var position: Int = 0
            private set

        fun parseReferenceTypeSignature() {
            when (peek()) {
                'L' -> parseClassTypeSignature()
                'T' -> parseTypeVariableSignature()
                '[' -> parseArrayTypeSignature()
                else -> fail("field signature", "expected ReferenceTypeSignature at offset $position")
            }
        }

        fun expectEnd(kind: String) {
            if (position != signature.length) {
                fail(kind, "has trailing characters at offset $position")
            }
        }

        private fun parseJavaTypeSignature() {
            when (peek()) {
                'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> position++
                else -> parseReferenceTypeSignature()
            }
        }

        private fun parseClassTypeSignature() {
            expect('L', "field signature")
            parseSimpleOrPackageClassSegment(allowTypeArguments = true)
            while (peek() == '/') {
                position++
                parseSimpleOrPackageClassSegment(allowTypeArguments = true)
            }
            while (peek() == '.') {
                position++
                parseSimpleClassTypeSignature()
            }
            expect(';', "field signature")
        }

        private fun parseSimpleOrPackageClassSegment(allowTypeArguments: Boolean) {
            parseIdentifier("field signature")
            if (allowTypeArguments && peek() == '<') {
                parseTypeArguments()
            }
        }

        private fun parseSimpleClassTypeSignature() {
            parseIdentifier("field signature")
            if (peek() == '<') {
                parseTypeArguments()
            }
        }

        private fun parseTypeArguments() {
            expect('<', "field signature")
            var count = 0
            while (peek() != '>') {
                if (peek() == null) {
                    fail("field signature", "missing '>' for type arguments")
                }
                parseTypeArgument()
                count++
            }
            if (count == 0) {
                fail("field signature", "type arguments must not be empty")
            }
            expect('>', "field signature")
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
            expect('T', "field signature")
            parseIdentifier("field signature")
            expect(';', "field signature")
        }

        private fun parseArrayTypeSignature() {
            expect('[', "field signature")
            parseJavaTypeSignature()
        }

        private fun parseIdentifier(kind: String): String {
            val start = position
            while (true) {
                val current = peek()
                if (current == null || current in charArrayOf('.', ';', '[', '/', '<', '>', ':')) {
                    break
                }
                position++
            }
            if (position == start) {
                fail(kind, "expected Identifier at offset $start")
            }
            return signature.substring(start, position)
        }

        private fun expect(
            expected: Char,
            kind: String,
        ) {
            if (peek() != expected) {
                fail(kind, "expected '$expected' at offset $position")
            }
            position++
        }

        private fun peek(): Char? =
            if (position < signature.length) {
                signature[position]
            } else {
                null
            }

        private fun fail(
            kind: String,
            reason: String,
        ): Nothing =
            throw ClassFileFormatException(
                "Invalid Signature attribute at $ownerPath: " +
                    "'$signature' is not a valid $kind: $reason",
            )
    }
}
