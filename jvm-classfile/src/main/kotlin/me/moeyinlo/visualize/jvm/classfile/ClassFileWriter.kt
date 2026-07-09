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

    fun writeHeader(version: ClassFileVersion): ByteArray {
        val writer = ClassFileByteWriter()
        writeHeader(version, writer)
        return writer.toByteArray()
    }

    fun writeConstantPool(constantPool: ConstantPool): ByteArray {
        val writer = ClassFileByteWriter()
        writeConstantPool(constantPool, writer)
        return writer.toByteArray()
    }

    fun writeFields(fields: List<FieldInfo>): ByteArray {
        val writer = ClassFileByteWriter()
        writeFields(fields, writer)
        return writer.toByteArray()
    }

    fun writeMethods(methods: List<MethodInfo>): ByteArray {
        val writer = ClassFileByteWriter()
        writeMethods(methods, writer)
        return writer.toByteArray()
    }

    fun writeAttributes(attributes: List<AttributeInfo>): ByteArray {
        val writer = ClassFileByteWriter()
        writeAttributes(attributes, writer)
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

    internal fun writeConstantPool(
        constantPool: ConstantPool,
        writer: ClassFileByteWriter,
    ) {
        writer.writeU2(constantPool.constantPoolCount)

        var index = 1
        while (index < constantPool.constantPoolCount) {
            when (val slot = constantPool.slotAt(ConstantPoolIndex(index))) {
                is ConstantPoolSlot.Entry -> {
                    writeConstantPoolEntry(slot.value, writer)
                    index += if (slot.value.occupiesTwoSlots) 2 else 1
                }

                ConstantPoolSlot.Unusable -> throw ClassFileFormatException(
                    "Invalid constant pool slot #$index: unusable slot without preceding two-slot entry",
                )
            }
        }
    }

    internal fun writeFields(
        fields: List<FieldInfo>,
        writer: ClassFileByteWriter,
    ) {
        writer.writeU2(fields.size)
        fields.forEachIndexed { index, field ->
            writer.writeU2(field.accessFlags)
                .writeConstantPoolIndex(field.nameIndex)
                .writeConstantPoolIndex(field.descriptorIndex)
            writeAttributes(field.attributes, writer, "fields[$index].attributes")
        }
    }

    internal fun writeMethods(
        methods: List<MethodInfo>,
        writer: ClassFileByteWriter,
    ) {
        writer.writeU2(methods.size)
        methods.forEachIndexed { index, method ->
            writer.writeU2(method.accessFlags)
                .writeConstantPoolIndex(method.nameIndex)
                .writeConstantPoolIndex(method.descriptorIndex)
            writeAttributes(method.attributes, writer, "methods[$index].attributes")
        }
    }

    internal fun writeAttributes(
        attributes: List<AttributeInfo>,
        writer: ClassFileByteWriter,
        ownerPath: String = "attributes",
    ) {
        writer.writeU2(attributes.size)
        attributes.forEachIndexed { index, attribute ->
            writeAttribute(attribute, writer, "$ownerPath[$index]")
        }
    }

    private fun writeConstantPoolEntry(
        entry: ConstantPoolEntry,
        writer: ClassFileByteWriter,
    ) {
        when (entry) {
            is ConstantUtf8Entry -> {
                val encodedBytes = entry.encodedBytes
                writer.writeU1(ConstantUtf8Tag)
                    .writeU2(encodedBytes.size)
                    .writeBytes(encodedBytes)
            }

            is ConstantIntegerEntry -> writer.writeU1(ConstantIntegerTag)
                .writeU4(entry.value.toLong() and 0xFFFF_FFFFL)

            is ConstantFloatEntry -> writer.writeU1(ConstantFloatTag)
                .writeU4(java.lang.Float.floatToRawIntBits(entry.value).toLong() and 0xFFFF_FFFFL)

            is ConstantLongEntry -> {
                val bits = entry.value
                writer.writeU1(ConstantLongTag)
                    .writeU4((bits ushr 32) and 0xFFFF_FFFFL)
                    .writeU4(bits and 0xFFFF_FFFFL)
            }

            is ConstantDoubleEntry -> {
                val bits = java.lang.Double.doubleToRawLongBits(entry.value)
                writer.writeU1(ConstantDoubleTag)
                    .writeU4((bits ushr 32) and 0xFFFF_FFFFL)
                    .writeU4(bits and 0xFFFF_FFFFL)
            }

            is ConstantClassEntry -> writer.writeU1(ConstantClassTag)
                .writeConstantPoolIndex(entry.nameIndex)

            is ConstantStringEntry -> writer.writeU1(ConstantStringTag)
                .writeConstantPoolIndex(entry.stringIndex)

            is ConstantNameAndTypeEntry -> writer.writeU1(ConstantNameAndTypeTag)
                .writeConstantPoolIndex(entry.nameIndex)
                .writeConstantPoolIndex(entry.descriptorIndex)

            is ConstantFieldRefEntry -> writer.writeU1(ConstantFieldRefTag)
                .writeMemberRef(entry)

            is ConstantMethodRefEntry -> writer.writeU1(ConstantMethodRefTag)
                .writeMemberRef(entry)

            is ConstantInterfaceMethodRefEntry -> writer.writeU1(ConstantInterfaceMethodRefTag)
                .writeMemberRef(entry)

            is ConstantMethodHandleEntry -> writer.writeU1(ConstantMethodHandleTag)
                .writeU1(entry.referenceKind.value)
                .writeConstantPoolIndex(entry.referenceIndex)

            is ConstantMethodTypeEntry -> writer.writeU1(ConstantMethodTypeTag)
                .writeConstantPoolIndex(entry.descriptorIndex)

            is ConstantDynamicEntry -> writer.writeU1(ConstantDynamicTag)
                .writeU2(entry.bootstrapMethodIndex.value)
                .writeConstantPoolIndex(entry.nameAndTypeIndex)

            is ConstantInvokeDynamicEntry -> writer.writeU1(ConstantInvokeDynamicTag)
                .writeU2(entry.bootstrapMethodIndex.value)
                .writeConstantPoolIndex(entry.nameAndTypeIndex)

            is ConstantModuleEntry -> writer.writeU1(ConstantModuleTag)
                .writeConstantPoolIndex(entry.nameIndex)

            is ConstantPackageEntry -> writer.writeU1(ConstantPackageTag)
                .writeConstantPoolIndex(entry.nameIndex)
        }
    }

    private fun ClassFileByteWriter.writeMemberRef(entry: ConstantMemberRefEntry): ClassFileByteWriter =
        writeConstantPoolIndex(entry.classIndex)
            .writeConstantPoolIndex(entry.nameAndTypeIndex)

    private fun ClassFileByteWriter.writeConstantPoolIndex(index: ConstantPoolIndex): ClassFileByteWriter =
        writeU2(index.value)

    private fun ClassFileByteWriter.writeOptionalConstantPoolIndex(index: ConstantPoolIndex?): ClassFileByteWriter =
        writeU2(index?.value ?: 0)

    private fun writeAttribute(
        attribute: AttributeInfo,
        writer: ClassFileByteWriter,
        ownerPath: String,
    ) {
        when (attribute) {
            is RawAttributeInfo -> writer.writeAttributeInfo(attribute.nameIndex, attribute.info)
            is UnknownAttributeInfo -> writer.writeAttributeInfo(attribute.nameIndex, attribute.info)
            is SyntheticAttribute -> writer.writeAttributeInfo(attribute.nameIndex, byteArrayOf())
            is DeprecatedAttribute -> writer.writeAttributeInfo(attribute.nameIndex, byteArrayOf())
            is SourceFileAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeConstantPoolIndex(attribute.sourceFileIndex)
            }
            is ConstantValueAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeConstantPoolIndex(attribute.constantValueIndex)
            }
            is ExceptionsAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.exceptionIndexTable.size)
                attribute.exceptionIndexTable.forEach { exceptionIndex ->
                    writeConstantPoolIndex(exceptionIndex)
                }
            }
            is InnerClassesAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.classes.size)
                attribute.classes.forEach { entry ->
                    writeConstantPoolIndex(entry.innerClassInfoIndex)
                    writeOptionalConstantPoolIndex(entry.outerClassInfoIndex)
                    writeOptionalConstantPoolIndex(entry.innerNameIndex)
                    writeU2(entry.innerClassAccessFlags)
                }
            }
            is EnclosingMethodAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeConstantPoolIndex(attribute.classIndex)
                writeOptionalConstantPoolIndex(attribute.methodIndex)
            }
            is SourceDebugExtensionAttribute -> writer.writeAttributeInfo(
                attribute.nameIndex,
                attribute.debugExtension,
            )
            is LineNumberTableAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.entries.size)
                attribute.entries.forEach { entry ->
                    writeU2(entry.startPc)
                    writeU2(entry.lineNumber)
                }
            }
            else -> throw UnsupportedOperationException(
                "Writing ${attribute::class.simpleName} at $ownerPath requires a specific attribute writer",
            )
        }
    }

    private fun ClassFileByteWriter.writeAttributeInfo(
        nameIndex: ConstantPoolIndex,
        info: ByteArray,
    ): ClassFileByteWriter =
        writeConstantPoolIndex(nameIndex)
            .writeU4(info.size.toLong())
            .writeBytes(info)

    private fun ClassFileByteWriter.writeAttributeInfo(
        nameIndex: ConstantPoolIndex,
        writeInfo: ClassFileByteWriter.() -> Unit,
    ): ClassFileByteWriter {
        val infoWriter = ClassFileByteWriter()
        infoWriter.writeInfo()
        return writeAttributeInfo(nameIndex, infoWriter.toByteArray())
    }
}
