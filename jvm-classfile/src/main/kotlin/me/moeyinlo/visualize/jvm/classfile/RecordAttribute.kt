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
        val runtimeInvisibleAnnotationsPaths = mutableListOf<String>()
        val runtimeVisibleTypeAnnotationsPaths = mutableListOf<String>()
        val runtimeInvisibleTypeAnnotationsPaths = mutableListOf<String>()
        val runtimeVisibleParameterAnnotationsPaths = mutableListOf<String>()
        val runtimeInvisibleParameterAnnotationsPaths = mutableListOf<String>()
        val codePaths = mutableListOf<String>()
        val constantValuePaths = mutableListOf<String>()
        val exceptionsPaths = mutableListOf<String>()
        val annotationDefaultPaths = mutableListOf<String>()
        val bootstrapMethodsPaths = mutableListOf<String>()
        val sourceFilePaths = mutableListOf<String>()
        val sourceDebugExtensionPaths = mutableListOf<String>()
        val innerClassesPaths = mutableListOf<String>()
        val enclosingMethodPaths = mutableListOf<String>()
        val methodParametersPaths = mutableListOf<String>()
        attributes.forEachIndexed { index, attribute ->
            val name = attributeName(context, attribute, "$ownerPath.attributes[$index].attribute_name_index")
            when (name) {
                "Signature" -> signaturePaths += "$ownerPath.attributes[$index]"
                "RuntimeVisibleAnnotations" -> runtimeVisibleAnnotationsPaths += "$ownerPath.attributes[$index]"
                "RuntimeInvisibleAnnotations" -> runtimeInvisibleAnnotationsPaths += "$ownerPath.attributes[$index]"
                "RuntimeVisibleTypeAnnotations" -> runtimeVisibleTypeAnnotationsPaths += "$ownerPath.attributes[$index]"
                "RuntimeInvisibleTypeAnnotations" -> runtimeInvisibleTypeAnnotationsPaths += "$ownerPath.attributes[$index]"
                "RuntimeVisibleParameterAnnotations" ->
                    runtimeVisibleParameterAnnotationsPaths += "$ownerPath.attributes[$index]"
                "RuntimeInvisibleParameterAnnotations" ->
                    runtimeInvisibleParameterAnnotationsPaths += "$ownerPath.attributes[$index]"
                "Code" -> codePaths += "$ownerPath.attributes[$index]"
                "ConstantValue" -> constantValuePaths += "$ownerPath.attributes[$index]"
                "Exceptions" -> exceptionsPaths += "$ownerPath.attributes[$index]"
                "AnnotationDefault" -> annotationDefaultPaths += "$ownerPath.attributes[$index]"
                "BootstrapMethods" -> bootstrapMethodsPaths += "$ownerPath.attributes[$index]"
                "SourceFile" -> sourceFilePaths += "$ownerPath.attributes[$index]"
                "SourceDebugExtension" -> sourceDebugExtensionPaths += "$ownerPath.attributes[$index]"
                "InnerClasses" -> innerClassesPaths += "$ownerPath.attributes[$index]"
                "EnclosingMethod" -> enclosingMethodPaths += "$ownerPath.attributes[$index]"
                "MethodParameters" -> methodParametersPaths += "$ownerPath.attributes[$index]"
            }
        }
        requireAbsentAttribute(codePaths, "Code", "method_info", ownerPath)
        requireAbsentAttribute(constantValuePaths, "ConstantValue", "field_info", ownerPath)
        requireAbsentAttribute(exceptionsPaths, "Exceptions", "method_info", ownerPath)
        requireAbsentAttribute(annotationDefaultPaths, "AnnotationDefault", "method_info", ownerPath)
        requireAbsentAttribute(bootstrapMethodsPaths, "BootstrapMethods", "ClassFile", ownerPath)
        requireAbsentAttribute(sourceFilePaths, "SourceFile", "ClassFile", ownerPath)
        requireAbsentAttribute(sourceDebugExtensionPaths, "SourceDebugExtension", "ClassFile", ownerPath)
        requireAbsentAttribute(innerClassesPaths, "InnerClasses", "ClassFile", ownerPath)
        requireAbsentAttribute(enclosingMethodPaths, "EnclosingMethod", "ClassFile", ownerPath)
        requireAbsentAttribute(methodParametersPaths, "MethodParameters", "method_info", ownerPath)
        requireAbsentAttribute(
            runtimeVisibleParameterAnnotationsPaths,
            "RuntimeVisibleParameterAnnotations",
            "method_info",
            ownerPath,
        )
        requireAbsentAttribute(
            runtimeInvisibleParameterAnnotationsPaths,
            "RuntimeInvisibleParameterAnnotations",
            "method_info",
            ownerPath,
        )
        requireAtMostOneAttribute(signaturePaths, "Signature", ownerPath)
        requireAtMostOneAttribute(runtimeVisibleAnnotationsPaths, "RuntimeVisibleAnnotations", ownerPath)
        requireAtMostOneAttribute(runtimeInvisibleAnnotationsPaths, "RuntimeInvisibleAnnotations", ownerPath)
        requireAtMostOneAttribute(runtimeVisibleTypeAnnotationsPaths, "RuntimeVisibleTypeAnnotations", ownerPath)
        requireAtMostOneAttribute(runtimeInvisibleTypeAnnotationsPaths, "RuntimeInvisibleTypeAnnotations", ownerPath)
    }


    private fun requireAbsentAttribute(
        paths: List<String>,
        attributeName: String,
        allowedLocation: String,
        ownerPath: String,
    ) {
        if (paths.isNotEmpty()) {
            throw ClassFileFormatException(
                "Invalid $ownerPath attributes: $attributeName is permitted only in " +
                    "$allowedLocation attributes but found at ${paths.joinToString()}",
            )
        }
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
