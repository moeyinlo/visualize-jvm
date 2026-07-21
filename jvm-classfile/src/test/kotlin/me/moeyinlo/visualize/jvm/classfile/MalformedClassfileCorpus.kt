package me.moeyinlo.visualize.jvm.classfile

data class MalformedClassfileCase(
    val name: String,
    val bytes: ByteArray,
    val expectedExceptionSimpleName: String,
    val expectedMessageFragment: String,
)

object MalformedClassfileCorpus {
    val cases: List<MalformedClassfileCase> = listOf(
        MalformedClassfileCase(
            name = "truncated header",
            bytes = bytes(0xCA, 0xFE),
            expectedExceptionSimpleName = "ClassFileReadException",
            expectedMessageFragment = "Unexpected end of classfile",
        ),
        MalformedClassfileCase(
            name = "bad magic",
            bytes = bytes(0xCA, 0xFE, 0xBA, 0xAD, 0x00, 0x00, 0x00, 0x46),
            expectedExceptionSimpleName = "ClassFileFormatException",
            expectedMessageFragment = "Invalid classfile magic",
        ),
        MalformedClassfileCase(
            name = "unsupported future major version",
            bytes = bytes(0xCA, 0xFE, 0xBA, 0xBE, 0x00, 0x00, 0x00, 0x47),
            expectedExceptionSimpleName = "ClassFileFormatException",
            expectedMessageFragment = "Unsupported classfile version",
        ),
        MalformedClassfileCase(
            name = "zero constant pool count",
            bytes = validJava26Header() + u2(0),
            expectedExceptionSimpleName = "ClassFileFormatException",
            expectedMessageFragment = "Invalid constant_pool_count=0",
        ),
        MalformedClassfileCase(
            name = "truncated UTF8 constant",
            bytes = validJava26Header() +
                u2(2) +
                bytes(1) +
                u2(3) +
                "A".encodeToByteArray(),
            expectedExceptionSimpleName = "ClassFileReadException",
            expectedMessageFragment = "Unexpected end of classfile",
        ),
    )

    private fun validJava26Header(): ByteArray =
        bytes(0xCA, 0xFE, 0xBA, 0xBE, 0x00, 0x00, 0x00, 0x46)

    private fun u2(value: Int): ByteArray =
        bytes((value ushr 8) and 0xFF, value and 0xFF)

    private fun bytes(vararg values: Int): ByteArray =
        values.map { value -> value.toByte() }.toByteArray()
}
