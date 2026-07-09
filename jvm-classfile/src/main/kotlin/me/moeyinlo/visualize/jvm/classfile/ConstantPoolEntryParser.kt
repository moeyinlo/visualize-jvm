package me.moeyinlo.visualize.jvm.classfile

class ConstantUtf8Entry(
    val value: String,
    encodedBytes: ByteArray,
) : ConstantPoolEntry {
    val encodedBytes: ByteArray = encodedBytes.copyOf()
}

data class ConstantIntegerEntry(val value: Int) : ConstantPoolEntry

data class ConstantFloatEntry(val value: Float) : ConstantPoolEntry

data class ConstantLongEntry(val value: Long) : ConstantPoolEntry {
    override val occupiesTwoSlots: Boolean = true
}

data class ConstantDoubleEntry(val value: Double) : ConstantPoolEntry {
    override val occupiesTwoSlots: Boolean = true
}

data class ConstantClassEntry(val nameIndex: ConstantPoolIndex) : ConstantPoolEntry

data class ConstantStringEntry(val stringIndex: ConstantPoolIndex) : ConstantPoolEntry

data class ConstantNameAndTypeEntry(
    val nameIndex: ConstantPoolIndex,
    val descriptorIndex: ConstantPoolIndex,
) : ConstantPoolEntry

sealed interface ConstantMemberRefEntry : ConstantPoolEntry {
    val classIndex: ConstantPoolIndex
    val nameAndTypeIndex: ConstantPoolIndex
}

data class ConstantFieldRefEntry(
    override val classIndex: ConstantPoolIndex,
    override val nameAndTypeIndex: ConstantPoolIndex,
) : ConstantMemberRefEntry

data class ConstantMethodRefEntry(
    override val classIndex: ConstantPoolIndex,
    override val nameAndTypeIndex: ConstantPoolIndex,
) : ConstantMemberRefEntry

data class ConstantInterfaceMethodRefEntry(
    override val classIndex: ConstantPoolIndex,
    override val nameAndTypeIndex: ConstantPoolIndex,
) : ConstantMemberRefEntry

enum class MethodHandleReferenceKind(val value: Int) {
    GetField(1),
    GetStatic(2),
    PutField(3),
    PutStatic(4),
    InvokeVirtual(5),
    InvokeStatic(6),
    InvokeSpecial(7),
    NewInvokeSpecial(8),
    InvokeInterface(9),
    ;

    companion object {
        fun fromValue(value: Int, source: String, offset: Int): MethodHandleReferenceKind =
            entries.firstOrNull { it.value == value }
                ?: throw ClassFileFormatException(
                    "Invalid CONSTANT_MethodHandle reference_kind=$value source=$source offset=$offset expected=1..9",
                )
    }
}

data class ConstantMethodHandleEntry(
    val referenceKind: MethodHandleReferenceKind,
    val referenceIndex: ConstantPoolIndex,
) : ConstantPoolEntry

data class ConstantMethodTypeEntry(val descriptorIndex: ConstantPoolIndex) : ConstantPoolEntry

@JvmInline
value class BootstrapMethodIndex(val value: Int) {
    init {
        require(value >= 0) { "Bootstrap method indexes are zero-based: $value" }
    }
}

data class ConstantDynamicEntry(
    val bootstrapMethodIndex: BootstrapMethodIndex,
    val nameAndTypeIndex: ConstantPoolIndex,
) : ConstantPoolEntry

data class ConstantInvokeDynamicEntry(
    val bootstrapMethodIndex: BootstrapMethodIndex,
    val nameAndTypeIndex: ConstantPoolIndex,
) : ConstantPoolEntry

data class ConstantModuleEntry(val nameIndex: ConstantPoolIndex) : ConstantPoolEntry

data class ConstantPackageEntry(val nameIndex: ConstantPoolIndex) : ConstantPoolEntry

object ConstantPoolEntryParser {
    private const val ConstantUtf8Tag = 1
    private const val ConstantIntegerTag = 3
    private const val ConstantFloatTag = 4
    private const val ConstantLongTag = 5
    private const val ConstantDoubleTag = 6
    private const val ConstantClassTag = 7
    private const val ConstantStringTag = 8
    private const val ConstantFieldRefTag = 9
    private const val ConstantMethodRefTag = 10
    private const val ConstantInterfaceMethodRefTag = 11
    private const val ConstantNameAndTypeTag = 12
    private const val ConstantMethodHandleTag = 15
    private const val ConstantMethodTypeTag = 16
    private const val ConstantDynamicTag = 17
    private const val ConstantInvokeDynamicTag = 18
    private const val ConstantModuleTag = 19
    private const val ConstantPackageTag = 20

    fun parseEntry(reader: ClassFileByteReader): ConstantPoolEntry {
        val entryOffset = reader.position
        val tag = reader.readU1()
        return when (tag) {
            ConstantUtf8Tag -> parseUtf8(reader)
            ConstantIntegerTag -> ConstantIntegerEntry(reader.readU4().toInt())
            ConstantFloatTag -> ConstantFloatEntry(java.lang.Float.intBitsToFloat(reader.readU4().toInt()))
            ConstantLongTag -> ConstantLongEntry(reader.readU8Bits())
            ConstantDoubleTag -> ConstantDoubleEntry(java.lang.Double.longBitsToDouble(reader.readU8Bits()))
            ConstantClassTag -> ConstantClassEntry(reader.readConstantPoolIndex())
            ConstantStringTag -> ConstantStringEntry(reader.readConstantPoolIndex())
            ConstantFieldRefTag -> parseMemberRef(reader, ::ConstantFieldRefEntry)
            ConstantMethodRefTag -> parseMemberRef(reader, ::ConstantMethodRefEntry)
            ConstantInterfaceMethodRefTag -> parseMemberRef(reader, ::ConstantInterfaceMethodRefEntry)
            ConstantNameAndTypeTag -> ConstantNameAndTypeEntry(
                nameIndex = reader.readConstantPoolIndex(),
                descriptorIndex = reader.readConstantPoolIndex(),
            )
            ConstantMethodHandleTag -> parseMethodHandle(reader)
            ConstantMethodTypeTag -> ConstantMethodTypeEntry(reader.readConstantPoolIndex())
            ConstantDynamicTag -> parseDynamic(reader, ::ConstantDynamicEntry)
            ConstantInvokeDynamicTag -> parseDynamic(reader, ::ConstantInvokeDynamicEntry)
            ConstantModuleTag -> ConstantModuleEntry(reader.readConstantPoolIndex())
            ConstantPackageTag -> ConstantPackageEntry(reader.readConstantPoolIndex())
            else -> throw ClassFileFormatException(
                "Unsupported constant pool tag source=${reader.source} offset=$entryOffset tag=$tag",
            )
        }
    }

    private fun parseUtf8(reader: ClassFileByteReader): ConstantUtf8Entry {
        val length = reader.readU2()
        val payloadOffset = reader.position
        val encodedBytes = reader.readSlice(length)
        val value = ModifiedUtf8.decode(
            encodedBytes = encodedBytes,
            source = reader.source,
            payloadOffset = payloadOffset,
        )
        return ConstantUtf8Entry(value = value, encodedBytes = encodedBytes)
    }

    private fun parseMemberRef(
        reader: ClassFileByteReader,
        create: (classIndex: ConstantPoolIndex, nameAndTypeIndex: ConstantPoolIndex) -> ConstantMemberRefEntry,
    ): ConstantMemberRefEntry = create(
        reader.readConstantPoolIndex(),
        reader.readConstantPoolIndex(),
    )

    private fun parseMethodHandle(reader: ClassFileByteReader): ConstantMethodHandleEntry {
        val referenceKindOffset = reader.position
        val referenceKind = MethodHandleReferenceKind.fromValue(
            value = reader.readU1(),
            source = reader.source,
            offset = referenceKindOffset,
        )
        return ConstantMethodHandleEntry(
            referenceKind = referenceKind,
            referenceIndex = reader.readConstantPoolIndex(),
        )
    }

    private fun parseDynamic(
        reader: ClassFileByteReader,
        create: (bootstrapMethodIndex: BootstrapMethodIndex, nameAndTypeIndex: ConstantPoolIndex) -> ConstantPoolEntry,
    ): ConstantPoolEntry = create(
        BootstrapMethodIndex(reader.readU2()),
        reader.readConstantPoolIndex(),
    )

    private fun ClassFileByteReader.readU8Bits(): Long {
        val highBytes = readU4()
        val lowBytes = readU4()
        return (highBytes shl 32) or lowBytes
    }

    private fun ClassFileByteReader.readConstantPoolIndex(): ConstantPoolIndex =
        ConstantPoolIndex(readU2())
}

private object ModifiedUtf8 {
    fun decode(
        encodedBytes: ByteArray,
        source: String,
        payloadOffset: Int,
    ): String {
        val builder = StringBuilder(encodedBytes.size)
        var index = 0

        while (index < encodedBytes.size) {
            val first = encodedBytes[index].toInt() and 0xFF
            when {
                first == 0 -> fail(source, payloadOffset + index, "zero byte is not valid in CONSTANT_Utf8")
                first and 0x80 == 0 -> {
                    builder.append(first.toChar())
                    index += 1
                }

                first and 0xE0 == 0xC0 -> {
                    val second = continuationByte(encodedBytes, index + 1, source, payloadOffset)
                    val value = ((first and 0x1F) shl 6) or (second and 0x3F)
                    if (value in 1..0x7F) {
                        fail(source, payloadOffset + index, "overlong two-byte encoding in CONSTANT_Utf8")
                    }
                    builder.append(value.toChar())
                    index += 2
                }

                first and 0xF0 == 0xE0 -> {
                    val second = continuationByte(encodedBytes, index + 1, source, payloadOffset)
                    val third = continuationByte(encodedBytes, index + 2, source, payloadOffset)
                    val value = ((first and 0x0F) shl 12) or ((second and 0x3F) shl 6) or (third and 0x3F)
                    if (value < 0x0800) {
                        fail(source, payloadOffset + index, "overlong three-byte encoding in CONSTANT_Utf8")
                    }
                    builder.append(value.toChar())
                    index += 3
                }

                else -> fail(source, payloadOffset + index, "unsupported leading byte in CONSTANT_Utf8")
            }
        }

        return builder.toString()
    }

    private fun continuationByte(
        encodedBytes: ByteArray,
        index: Int,
        source: String,
        payloadOffset: Int,
    ): Int {
        if (index >= encodedBytes.size) {
            fail(source, payloadOffset + index, "truncated modified UTF-8 sequence in CONSTANT_Utf8")
        }

        val byte = encodedBytes[index].toInt() and 0xFF
        if (byte and 0xC0 != 0x80) {
            fail(source, payloadOffset + index, "invalid continuation byte in CONSTANT_Utf8")
        }
        return byte
    }

    private fun fail(source: String, offset: Int, reason: String): Nothing =
        throw ClassFileFormatException("$reason source=$source offset=$offset")
}
