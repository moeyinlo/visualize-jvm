package me.moeyinlo.visualize.jvm.classfile

import java.io.ByteArrayOutputStream

class ClassFileByteWriter {
    private val output = ByteArrayOutputStream()

    val position: Int
        get() = output.size()

    fun writeU1(value: Int): ClassFileByteWriter {
        require(value in 0..0xFF) { "u1 value out of range: $value" }
        output.write(value)
        return this
    }

    fun writeU2(value: Int): ClassFileByteWriter {
        require(value in 0..0xFFFF) { "u2 value out of range: $value" }
        output.write((value ushr 8) and 0xFF)
        output.write(value and 0xFF)
        return this
    }

    fun writeU4(value: Long): ClassFileByteWriter {
        require(value in 0..0xFFFF_FFFFL) { "u4 value out of range: $value" }
        output.write(((value ushr 24) and 0xFF).toInt())
        output.write(((value ushr 16) and 0xFF).toInt())
        output.write(((value ushr 8) and 0xFF).toInt())
        output.write((value and 0xFF).toInt())
        return this
    }

    fun writeBytes(bytes: ByteArray): ClassFileByteWriter {
        output.write(bytes.copyOf())
        return this
    }

    fun toByteArray(): ByteArray = output.toByteArray()
}

object ClassFileWriter {
    fun writeHeader(version: ClassFileVersion): ByteArray {
        val writer = ClassFileByteWriter()
        writeHeader(version, writer)
        return writer.toByteArray()
    }

    internal fun writeHeader(
        version: ClassFileVersion,
        writer: ClassFileByteWriter,
    ) {
        val supportedVersion = ClassFileHeaderParser.validateJava26Version(version)
        writer.writeU4(ClassFileHeaderParser.ExpectedMagic)
            .writeU2(supportedVersion.minor)
            .writeU2(supportedVersion.major)
    }
}
