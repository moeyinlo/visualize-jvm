package me.moeyinlo.visualize.jvm.classfile

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

class SourceDebugExtensionAttribute(
    override val nameIndex: ConstantPoolIndex,
    debugExtension: ByteArray,
    val text: String,
) : AttributeInfo {
    private val debugExtensionBytes = debugExtension.copyOf()

    val debugExtension: ByteArray
        get() = debugExtensionBytes.copyOf()
}

object SourceDebugExtensionAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val bytes = context.reader.readSlice(context.reader.remaining)
        return SourceDebugExtensionAttribute(
            nameIndex = context.nameIndex,
            debugExtension = bytes,
            text = decodeUtf8(bytes, context.ownerPath),
        )
    }

    private fun decodeUtf8(
        bytes: ByteArray,
        ownerPath: String,
    ): String {
        val decoder = StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (exception: CharacterCodingException) {
            throw ClassFileFormatException(
                "Invalid SourceDebugExtension UTF-8 payload at $ownerPath: ${exception.message}",
            )
        }
    }
}
