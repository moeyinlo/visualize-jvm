package me.moeyinlo.visualize.jvm.classfile

class ClassFileFormatException(message: String) : RuntimeException(message)

data class ClassFileMagic(
    val offset: Int,
    val value: Long,
)

data class ClassFileVersion(
    val offset: Int,
    val minor: Int,
    val major: Int,
)

object ClassFileHeaderParser {
    const val ExpectedMagic: Long = 0xCAFEBABEL

    fun parseMagic(reader: ClassFileByteReader): ClassFileMagic {
        val offset = reader.position
        val actual = reader.readU4()
        if (actual != ExpectedMagic) {
            throw ClassFileFormatException(
                "Invalid classfile magic source=${reader.source} offset=$offset " +
                    "expected=${ExpectedMagic.toHexU4()} actual=${actual.toHexU4()}",
            )
        }
        return ClassFileMagic(offset = offset, value = actual)
    }

    fun parseVersion(reader: ClassFileByteReader): ClassFileVersion {
        val offset = reader.position
        val minor = reader.readU2()
        val major = reader.readU2()
        return ClassFileVersion(offset = offset, minor = minor, major = major)
    }

    private fun Long.toHexU4(): String = "0x" + toString(16).uppercase().padStart(8, '0')
}
