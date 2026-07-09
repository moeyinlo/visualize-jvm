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
) {
    val dependsOnJava26PreviewFeatures: Boolean
        get() = major == ClassFileHeaderParser.Java26MajorVersion &&
            minor == ClassFileHeaderParser.PreviewMinorVersion
}

object ClassFileHeaderParser {
    const val ExpectedMagic: Long = 0xCAFEBABEL
    const val MinimumSupportedMajorVersion: Int = 45
    const val Java12MajorVersion: Int = 56
    const val Java26MajorVersion: Int = 70
    const val PreviewMinorVersion: Int = 65535

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

    fun validateJava26Version(version: ClassFileVersion): ClassFileVersion {
        if (version.major !in MinimumSupportedMajorVersion..Java26MajorVersion) {
            throw ClassFileFormatException(
                "Unsupported classfile version offset=${version.offset} major=${version.major} " +
                    "minor=${version.minor} supported=$MinimumSupportedMajorVersion..$Java26MajorVersion",
            )
        }

        if (version.major >= Java12MajorVersion && version.minor != 0 && version.minor != PreviewMinorVersion) {
            throw ClassFileFormatException(
                "Unsupported classfile version offset=${version.offset} major=${version.major} " +
                    "minor=${version.minor}; for major >= $Java12MajorVersion, minor must be 0 or $PreviewMinorVersion",
            )
        }

        if (version.major in Java12MajorVersion until Java26MajorVersion && version.minor == PreviewMinorVersion) {
            throw ClassFileFormatException(
                "Unsupported classfile version offset=${version.offset} major=${version.major} " +
                    "minor=${version.minor}; older preview classfiles are not loadable by Java SE 26",
            )
        }

        return version
    }

    private fun Long.toHexU4(): String = "0x" + toString(16).uppercase().padStart(8, '0')
}
