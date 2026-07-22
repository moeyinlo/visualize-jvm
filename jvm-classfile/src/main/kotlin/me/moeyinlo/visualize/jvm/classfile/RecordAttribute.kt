package me.moeyinlo.visualize.jvm.classfile

data class RecordAttribute(
    override val nameIndex: ConstantPoolIndex,
    val components: List<RecordComponentInfo>,
) : AttributeInfo

data class RecordComponentInfo(
    val nameIndex: ConstantPoolIndex,
    val descriptorIndex: ConstantPoolIndex,
    val attributes: List<AttributeInfo>,
)

object RecordAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val componentsCount = context.reader.readU2()
        return RecordAttribute(
            nameIndex = context.nameIndex,
            components = List(componentsCount) { index ->
                parseComponent(context, "${context.ownerPath}.components[$index]")
            },
        )
    }

    private fun parseComponent(
        context: AttributeParseContext,
        ownerPath: String,
    ): RecordComponentInfo {
        val nameIndex = readNameIndex(context, "$ownerPath.name_index")
        val descriptorIndex = readDescriptorIndex(context, "$ownerPath.descriptor_index")
        val attributes = AttributeInfoParser.parseAttributes(
            reader = context.reader,
            constantPool = context.constantPool,
            registry = context.registry,
            ownerPath = ownerPath,
        )
        validateComponentAttributes(context, attributes, ownerPath)
        return RecordComponentInfo(
            nameIndex = nameIndex,
            descriptorIndex = descriptorIndex,
            attributes = attributes,
        )
    }

    private fun validateComponentAttributes(
        context: AttributeParseContext,
        attributes: List<AttributeInfo>,
        ownerPath: String,
    ) {
        val signaturePaths = mutableListOf<String>()
        val runtimeVisibleAnnotationsPaths = mutableListOf<String>()
        attributes.forEachIndexed { index, attribute ->
            val name = attributeName(context, attribute, "$ownerPath.attributes[$index].attribute_name_index")
            when (name) {
                "Signature" -> signaturePaths += "$ownerPath.attributes[$index]"
                "RuntimeVisibleAnnotations" -> runtimeVisibleAnnotationsPaths += "$ownerPath.attributes[$index]"
            }
        }
        requireAtMostOneAttribute(signaturePaths, "Signature", ownerPath)
        requireAtMostOneAttribute(runtimeVisibleAnnotationsPaths, "RuntimeVisibleAnnotations", ownerPath)
    }


    private fun requireAtMostOneAttribute(
        paths: List<String>,
        attributeName: String,
        ownerPath: String,
    ) {
        if (paths.size <= 1) {
            return
        }
        throw ClassFileFormatException(
            "Invalid $ownerPath attributes: at most one $attributeName attribute is permitted " +
                "but found ${paths.size} at ${paths.joinToString()}",
        )
    }
    private fun attributeName(
        context: AttributeParseContext,
        attribute: AttributeInfo,
        role: String,
    ): String {
        val entry = try {
            context.constantPool[attribute.nameIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException("Invalid $role=${attribute.nameIndex}: ${exception.message}")
        }
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $role=${attribute.nameIndex}: expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return entry.value
    }

    private fun readNameIndex(
        context: AttributeParseContext,
        role: String,
    ): ConstantPoolIndex {
        val (index, entry) = readUtf8Reference(context, role)
        ClassNameValidator.validateUnqualifiedName(index, role, entry.value)
        return index
    }

    private fun readDescriptorIndex(
        context: AttributeParseContext,
        role: String,
    ): ConstantPoolIndex {
        val (index, entry) = readUtf8Reference(context, role)
        DescriptorValidator.validateFieldDescriptor(index, role, entry.value)
        return index
    }

    private fun readUtf8Reference(
        context: AttributeParseContext,
        role: String,
    ): Pair<ConstantPoolIndex, ConstantUtf8Entry> {
        val index = RawAttributeInfoParser.readNonZeroConstantPoolIndex(context.reader, role)
        val entry = try {
            context.constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException("Invalid $role=$index: ${exception.message}")
        }
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $role=$index: expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return index to entry
    }
}
