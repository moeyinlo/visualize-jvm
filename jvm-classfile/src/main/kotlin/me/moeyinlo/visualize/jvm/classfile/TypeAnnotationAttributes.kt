package me.moeyinlo.visualize.jvm.classfile

data class RuntimeVisibleTypeAnnotationsAttribute(
    override val nameIndex: ConstantPoolIndex,
    val annotations: List<TypeAnnotationInfo>,
) : AttributeInfo

data class RuntimeInvisibleTypeAnnotationsAttribute(
    override val nameIndex: ConstantPoolIndex,
    val annotations: List<TypeAnnotationInfo>,
) : AttributeInfo

data class TypeAnnotationInfo(
    val targetType: Int,
    val targetInfo: TypeAnnotationTargetInfo,
    val targetPath: TypePath,
    val annotation: AnnotationInfo,
)

sealed interface TypeAnnotationTargetInfo {
    data class TypeParameterTarget(val typeParameterIndex: Int) : TypeAnnotationTargetInfo
    data class SupertypeTarget(val supertypeIndex: Int) : TypeAnnotationTargetInfo
    data class TypeParameterBoundTarget(
        val typeParameterIndex: Int,
        val boundIndex: Int,
    ) : TypeAnnotationTargetInfo

    data object EmptyTarget : TypeAnnotationTargetInfo
    data class FormalParameterTarget(val formalParameterIndex: Int) : TypeAnnotationTargetInfo
    data class ThrowsTarget(val throwsTypeIndex: Int) : TypeAnnotationTargetInfo
    data class LocalVarTarget(val table: List<LocalVarTargetTableEntry>) : TypeAnnotationTargetInfo
    data class CatchTarget(val exceptionTableIndex: Int) : TypeAnnotationTargetInfo
    data class OffsetTarget(val offset: Int) : TypeAnnotationTargetInfo
    data class TypeArgumentTarget(
        val offset: Int,
        val typeArgumentIndex: Int,
    ) : TypeAnnotationTargetInfo
}

data class LocalVarTargetTableEntry(
    val startPc: Int,
    val length: Int,
    val index: Int,
)

data class TypePath(
    val entries: List<TypePathEntry>,
)

data class TypePathEntry(
    val typePathKind: Int,
    val typeArgumentIndex: Int,
)

object RuntimeVisibleTypeAnnotationsAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo =
        RuntimeVisibleTypeAnnotationsAttribute(
            nameIndex = context.nameIndex,
            annotations = TypeAnnotationParser.parseTypeAnnotations(context, context.ownerPath),
        )
}

object RuntimeInvisibleTypeAnnotationsAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo =
        RuntimeInvisibleTypeAnnotationsAttribute(
            nameIndex = context.nameIndex,
            annotations = TypeAnnotationParser.parseTypeAnnotations(context, context.ownerPath),
        )
}

private object TypeAnnotationParser {
    private val ClassFileTargetTypes = setOf(0x00, 0x10, 0x11)
    private val FieldOrRecordComponentTargetTypes = setOf(0x13)
    private val MethodTargetTypes = setOf(0x01, 0x12, 0x14, 0x15, 0x16, 0x17)
    private val CodeTargetTypes = (0x40..0x4B).toSet()
    private val ClassFileAttributeAnnotationPath =
        Regex("""^ClassFile\.attributes\[\d+\]\.annotations\[\d+\]$""")
    private val FieldAttributeAnnotationPath =
        Regex("""^fields\[\d+\]\.attributes\[\d+\]\.annotations\[\d+\]$""")
    private val RecordComponentAttributeAnnotationPath =
        Regex("""^ClassFile\.attributes\[\d+\]\.components\[\d+\]\.attributes\[\d+\]\.annotations\[\d+\]$""")
    private val MethodAttributeAnnotationPath =
        Regex("""^methods\[\d+\]\.attributes\[\d+\]\.annotations\[\d+\]$""")
    private val CodeAttributeAnnotationPath =
        Regex("""^methods\[\d+\]\.attributes\[\d+\]\.attributes\[\d+\]\.annotations\[\d+\]$""")

    fun parseTypeAnnotations(
        context: AttributeParseContext,
        ownerPath: String,
    ): List<TypeAnnotationInfo> {
        val numAnnotations = context.reader.readU2()
        return List(numAnnotations) { index ->
            parseTypeAnnotation(context, "$ownerPath.annotations[$index]")
        }
    }

    private fun parseTypeAnnotation(
        context: AttributeParseContext,
        ownerPath: String,
    ): TypeAnnotationInfo {
        val targetType = context.reader.readU1()
        requireTargetTypeAllowedForOwner(ownerPath, targetType)
        return TypeAnnotationInfo(
            targetType = targetType,
            targetInfo = parseTargetInfo(context, ownerPath, targetType),
            targetPath = parseTypePath(context, "$ownerPath.target_path"),
            annotation = AnnotationParser.parseAnnotation(context, ownerPath),
        )
    }

    private fun requireTargetTypeAllowedForOwner(
        ownerPath: String,
        targetType: Int,
    ) {
        when {
            isClassFileAttribute(ownerPath) && targetType !in ClassFileTargetTypes -> {
                throw ClassFileFormatException(
                    "Invalid type_annotation target_type=${targetType.toHex()} at $ownerPath: " +
                        "ClassFile type annotations allow only target_type 0x00, 0x10, or 0x11",
                )
            }
            isFieldOrRecordComponentAttribute(ownerPath) && targetType !in FieldOrRecordComponentTargetTypes -> {
                throw ClassFileFormatException(
                    "Invalid type_annotation target_type=${targetType.toHex()} at $ownerPath: " +
                        "field_info and record_component_info type annotations allow only target_type 0x13",
                )
            }
            isMethodAttribute(ownerPath) && targetType !in MethodTargetTypes -> {
                throw ClassFileFormatException(
                    "Invalid type_annotation target_type=${targetType.toHex()} at $ownerPath: " +
                        "method_info type annotations allow only target_type 0x01, 0x12, 0x14, 0x15, 0x16, or 0x17",
                )
            }
            isCodeAttribute(ownerPath) && targetType !in CodeTargetTypes -> {
                throw ClassFileFormatException(
                    "Invalid type_annotation target_type=${targetType.toHex()} at $ownerPath: " +
                        "Code type annotations allow only target_type 0x40 through 0x4B",
                )
            }
        }
    }

    private fun isClassFileAttribute(ownerPath: String): Boolean =
        ClassFileAttributeAnnotationPath.matches(ownerPath)

    private fun isFieldOrRecordComponentAttribute(ownerPath: String): Boolean =
        FieldAttributeAnnotationPath.matches(ownerPath) ||
            RecordComponentAttributeAnnotationPath.matches(ownerPath)

    private fun isMethodAttribute(ownerPath: String): Boolean =
        MethodAttributeAnnotationPath.matches(ownerPath)

    private fun isCodeAttribute(ownerPath: String): Boolean =
        CodeAttributeAnnotationPath.matches(ownerPath)

    private fun Int.toHex(): String = "0x${toString(16).padStart(2, '0')}"

    private fun parseTargetInfo(
        context: AttributeParseContext,
        ownerPath: String,
        targetType: Int,
    ): TypeAnnotationTargetInfo =
        when (targetType) {
            0x00,
            0x01,
            -> TypeAnnotationTargetInfo.TypeParameterTarget(
                typeParameterIndex = context.reader.readU1(),
            )
            0x10 -> TypeAnnotationTargetInfo.SupertypeTarget(
                supertypeIndex = context.reader.readU2(),
            )
            0x11,
            0x12,
            -> TypeAnnotationTargetInfo.TypeParameterBoundTarget(
                typeParameterIndex = context.reader.readU1(),
                boundIndex = context.reader.readU1(),
            )
            0x13,
            0x14,
            0x15,
            -> TypeAnnotationTargetInfo.EmptyTarget
            0x16 -> TypeAnnotationTargetInfo.FormalParameterTarget(
                formalParameterIndex = context.reader.readU1(),
            )
            0x17 -> TypeAnnotationTargetInfo.ThrowsTarget(
                throwsTypeIndex = context.reader.readU2(),
            )
            0x40,
            0x41,
            -> parseLocalVarTarget(context)
            0x42 -> TypeAnnotationTargetInfo.CatchTarget(
                exceptionTableIndex = context.reader.readU2(),
            )
            in 0x43..0x46 -> TypeAnnotationTargetInfo.OffsetTarget(
                offset = context.reader.readU2(),
            )
            in 0x47..0x4B -> TypeAnnotationTargetInfo.TypeArgumentTarget(
                offset = context.reader.readU2(),
                typeArgumentIndex = context.reader.readU1(),
            )
            else -> throw ClassFileFormatException(
                "Invalid type_annotation target_type=0x${targetType.toString(16).padStart(2, '0')} at $ownerPath",
            )
        }

    private fun parseLocalVarTarget(context: AttributeParseContext): TypeAnnotationTargetInfo.LocalVarTarget {
        val tableLength = context.reader.readU2()
        return TypeAnnotationTargetInfo.LocalVarTarget(
            table = List(tableLength) {
                LocalVarTargetTableEntry(
                    startPc = context.reader.readU2(),
                    length = context.reader.readU2(),
                    index = context.reader.readU2(),
                )
            },
        )
    }

    private fun parseTypePath(
        context: AttributeParseContext,
        ownerPath: String,
    ): TypePath {
        val pathLength = context.reader.readU1()
        return TypePath(
            entries = List(pathLength) { index ->
                parseTypePathEntry(context, "$ownerPath.path[$index]")
            },
        )
    }

    private fun parseTypePathEntry(
        context: AttributeParseContext,
        ownerPath: String,
    ): TypePathEntry {
        val typePathKind = context.reader.readU1()
        val typeArgumentIndex = context.reader.readU1()
        if (typePathKind !in 0..3) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.type_path_kind=$typePathKind: expected 0..3",
            )
        }
        if (typePathKind != 3 && typeArgumentIndex != 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.type_argument_index=$typeArgumentIndex: expected 0 for type_path_kind=$typePathKind",
            )
        }
        return TypePathEntry(
            typePathKind = typePathKind,
            typeArgumentIndex = typeArgumentIndex,
        )
    }
}
