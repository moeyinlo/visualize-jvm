package me.moeyinlo.visualize.jvm.classfile

data class FieldInfo(
    val accessFlags: Int,
    val nameIndex: ConstantPoolIndex,
    val descriptorIndex: ConstantPoolIndex,
    val attributes: List<AttributeInfo>,
)

object FieldInfoParser {
    fun parseFields(reader: ClassFileByteReader): List<FieldInfo> =
        parseFields(reader) { attributeReader, ownerPath ->
            RawAttributeInfoParser.parseAttributes(attributeReader, ownerPath)
        }

    fun parseFields(
        reader: ClassFileByteReader,
        constantPool: ConstantPool,
        attributeParsers: AttributeParserRegistry,
        classKind: ClassFileKind = ClassFileKind.Class,
        majorVersion: Int = 70,
    ): List<FieldInfo> {
        val fields = parseFields(reader) { attributeReader, ownerPath ->
            AttributeInfoParser.parseAttributes(
                reader = attributeReader,
                constantPool = constantPool,
                registry = attributeParsers,
                ownerPath = ownerPath,
                majorVersion = majorVersion,
            )
        }
        validateFields(fields, constantPool, classKind)
        return fields
    }

    private fun parseFields(
        reader: ClassFileByteReader,
        parseAttributes: (reader: ClassFileByteReader, ownerPath: String) -> List<AttributeInfo>,
    ): List<FieldInfo> {
        val fieldsCount = reader.readU2()
        return List(fieldsCount) { index ->
            parseField(reader, index, parseAttributes)
        }
    }

    private fun parseField(
        reader: ClassFileByteReader,
        index: Int,
        parseAttributes: (reader: ClassFileByteReader, ownerPath: String) -> List<AttributeInfo>,
    ): FieldInfo {
        val ownerPath = "fields[$index]"
        return FieldInfo(
            accessFlags = reader.readU2(),
            nameIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(reader, "$ownerPath.name_index"),
            descriptorIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(reader, "$ownerPath.descriptor_index"),
            attributes = parseAttributes(reader, ownerPath),
        )
    }

    private fun validateFields(
        fields: List<FieldInfo>,
        constantPool: ConstantPool,
        classKind: ClassFileKind,
    ) {
        val seenFields = mutableMapOf<Pair<String, String>, Int>()
        fields.forEachIndexed { index, field ->
            val ownerPath = "fields[$index]"
            validateAccessFlags(field.accessFlags, classKind, ownerPath)
            val name = expectUtf8(constantPool, field.nameIndex, "$ownerPath.name_index")
            val descriptor = expectUtf8(constantPool, field.descriptorIndex, "$ownerPath.descriptor_index")
            ClassNameValidator.validateUnqualifiedName(field.nameIndex, "$ownerPath.name_index", name.value)
            DescriptorValidator.validateFieldDescriptor(field.descriptorIndex, "$ownerPath.descriptor_index", descriptor.value)
            validateFieldAttributes(field, constantPool, ownerPath)

            val duplicateOf = seenFields.putIfAbsent(name.value to descriptor.value, index)
            if (duplicateOf != null) {
                throw ClassFileFormatException(
                    "Duplicate field_info at $ownerPath: " +
                        "name='${name.value}' descriptor='${descriptor.value}' already used by fields[$duplicateOf]",
                )
            }
        }
    }

    private fun validateFieldAttributes(
        field: FieldInfo,
        constantPool: ConstantPool,
        ownerPath: String,
    ) {
        val signaturePaths = mutableListOf<String>()
        field.attributes.forEachIndexed { index, attribute ->
            when (attributeName(attribute, constantPool, "$ownerPath.attributes[$index].attribute_name_index")) {
                "Signature" -> signaturePaths += "$ownerPath.attributes[$index]"
            }
        }
        requireAtMostOneAttribute(signaturePaths, "Signature", ownerPath)
    }

    private fun requireAtMostOneAttribute(
        paths: List<String>,
        attributeName: String,
        ownerPath: String,
    ) {
        if (paths.size > 1) {
            throw ClassFileFormatException(
                "Invalid $ownerPath attributes: at most one $attributeName attribute is permitted " +
                    "but found ${paths.size} at ${paths.joinToString()}",
            )
        }
    }

    private fun attributeName(
        attribute: AttributeInfo,
        constantPool: ConstantPool,
        role: String,
    ): String =
        expectUtf8(constantPool, attribute.nameIndex, role).value

    private fun validateAccessFlags(
        accessFlags: Int,
        classKind: ClassFileKind,
        ownerPath: String,
    ) {
        val visibilityFlags = listOf(FieldAccessFlag.Public, FieldAccessFlag.Private, FieldAccessFlag.Protected)
            .filter { has(accessFlags, it) }
        if (visibilityFlags.size > 1) {
            failAccess(ownerPath, "must not set more than one of ACC_PUBLIC, ACC_PRIVATE, and ACC_PROTECTED")
        }

        if (has(accessFlags, FieldAccessFlag.Final) && has(accessFlags, FieldAccessFlag.Volatile)) {
            failAccess(ownerPath, "must not set both ACC_FINAL and ACC_VOLATILE")
        }

        if (classKind == ClassFileKind.Interface || classKind == ClassFileKind.AnnotationInterface) {
            val required = FieldAccessFlag.Public.mask or FieldAccessFlag.Static.mask or FieldAccessFlag.Final.mask
            if (accessFlags and required != required) {
                failAccess(ownerPath, "interface fields must set ACC_PUBLIC, ACC_STATIC, and ACC_FINAL")
            }
            val allowed = required or FieldAccessFlag.Synthetic.mask
            val disallowed = accessFlags and FieldAccessFlag.assignedMask and allowed.inv()
            if (disallowed != 0) {
                failAccess(ownerPath, "interface fields must not set ${FieldAccessFlag.namesFor(disallowed)}")
            }
        }
    }

    private fun expectUtf8(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        role: String,
    ): ConstantUtf8Entry {
        val entry = try {
            constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException("Invalid $role=$index: ${exception.message}")
        }
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $role=$index: expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return entry
    }

    private fun has(accessFlags: Int, flag: FieldAccessFlag): Boolean =
        accessFlags and flag.mask != 0

    private fun failAccess(
        ownerPath: String,
        reason: String,
    ): Nothing =
        throw ClassFileFormatException("Invalid $ownerPath.access_flags: $reason")

    private enum class FieldAccessFlag(
        val mask: Int,
        val specName: String,
    ) {
        Public(0x0001, "ACC_PUBLIC"),
        Private(0x0002, "ACC_PRIVATE"),
        Protected(0x0004, "ACC_PROTECTED"),
        Static(0x0008, "ACC_STATIC"),
        Final(0x0010, "ACC_FINAL"),
        Volatile(0x0040, "ACC_VOLATILE"),
        Transient(0x0080, "ACC_TRANSIENT"),
        Synthetic(0x1000, "ACC_SYNTHETIC"),
        Enum(0x4000, "ACC_ENUM"),
        ;

        companion object {
            val assignedMask: Int = entries.fold(0) { mask, flag -> mask or flag.mask }

            fun namesFor(raw: Int): String =
                entries
                    .filter { raw and it.mask != 0 }
                    .joinToString(", ") { it.specName }
        }
    }
}
