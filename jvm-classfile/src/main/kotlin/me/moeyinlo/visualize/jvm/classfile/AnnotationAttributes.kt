package me.moeyinlo.visualize.jvm.classfile

data class RuntimeVisibleAnnotationsAttribute(
    override val nameIndex: ConstantPoolIndex,
    val annotations: List<AnnotationInfo>,
) : AttributeInfo

data class RuntimeInvisibleAnnotationsAttribute(
    override val nameIndex: ConstantPoolIndex,
    val annotations: List<AnnotationInfo>,
) : AttributeInfo

data class AnnotationInfo(
    val typeIndex: ConstantPoolIndex,
    val elementValuePairs: List<ElementValuePair>,
)

data class ElementValuePair(
    val elementNameIndex: ConstantPoolIndex,
    val value: ElementValue,
)

sealed interface ElementValue {
    val tag: Char

    data class Const(
        override val tag: Char,
        val constValueIndex: ConstantPoolIndex,
    ) : ElementValue

    data class EnumConst(
        val typeNameIndex: ConstantPoolIndex,
        val constNameIndex: ConstantPoolIndex,
    ) : ElementValue {
        override val tag: Char = 'e'
    }

    data class ClassInfo(
        val classInfoIndex: ConstantPoolIndex,
    ) : ElementValue {
        override val tag: Char = 'c'
    }

    data class NestedAnnotation(
        val annotation: AnnotationInfo,
    ) : ElementValue {
        override val tag: Char = '@'
    }

    data class ArrayValue(
        val values: List<ElementValue>,
    ) : ElementValue {
        override val tag: Char = '['
    }
}

object RuntimeVisibleAnnotationsAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo =
        RuntimeVisibleAnnotationsAttribute(
            nameIndex = context.nameIndex,
            annotations = AnnotationParser.parseAnnotations(context, context.ownerPath),
        )
}

object RuntimeInvisibleAnnotationsAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo =
        RuntimeInvisibleAnnotationsAttribute(
            nameIndex = context.nameIndex,
            annotations = AnnotationParser.parseAnnotations(context, context.ownerPath),
        )
}

internal object AnnotationParser {
    fun parseAnnotations(
        context: AttributeParseContext,
        ownerPath: String,
    ): List<AnnotationInfo> {
        val numAnnotations = context.reader.readU2()
        return List(numAnnotations) { index ->
            parseAnnotation(context, "$ownerPath.annotations[$index]")
        }
    }

    private fun parseAnnotation(
        context: AttributeParseContext,
        ownerPath: String,
    ): AnnotationInfo {
        val typeIndex = readUtf8Index(context, "$ownerPath.type_index")
        val numElementValuePairs = context.reader.readU2()
        val pairs = List(numElementValuePairs) { index ->
            parseElementValuePair(context, "$ownerPath.element_value_pairs[$index]")
        }
        return AnnotationInfo(
            typeIndex = typeIndex,
            elementValuePairs = pairs,
        )
    }

    private fun parseElementValuePair(
        context: AttributeParseContext,
        ownerPath: String,
    ): ElementValuePair =
        ElementValuePair(
            elementNameIndex = readUtf8Index(context, "$ownerPath.element_name_index"),
            value = parseElementValue(context, "$ownerPath.value"),
        )

    fun parseElementValue(
        context: AttributeParseContext,
        ownerPath: String,
    ): ElementValue {
        val tag = context.reader.readU1().toChar()
        return when (tag) {
            'B',
            'C',
            'I',
            'S',
            'Z',
            -> ElementValue.Const(tag, readConstValueIndex<ConstantIntegerEntry>(context, ownerPath, tag))

            'D' -> ElementValue.Const(tag, readConstValueIndex<ConstantDoubleEntry>(context, ownerPath, tag))
            'F' -> ElementValue.Const(tag, readConstValueIndex<ConstantFloatEntry>(context, ownerPath, tag))
            'J' -> ElementValue.Const(tag, readConstValueIndex<ConstantLongEntry>(context, ownerPath, tag))
            's' -> ElementValue.Const(tag, readConstValueIndex<ConstantUtf8Entry>(context, ownerPath, tag))
            'e' -> ElementValue.EnumConst(
                typeNameIndex = readUtf8Index(context, "$ownerPath.enum_const_value.type_name_index"),
                constNameIndex = readUtf8Index(context, "$ownerPath.enum_const_value.const_name_index"),
            )
            'c' -> ElementValue.ClassInfo(
                classInfoIndex = readUtf8Index(context, "$ownerPath.class_info_index"),
            )
            '@' -> ElementValue.NestedAnnotation(
                annotation = parseAnnotation(context, "$ownerPath.annotation_value"),
            )
            '[' -> {
                val numValues = context.reader.readU2()
                ElementValue.ArrayValue(
                    values = List(numValues) { index ->
                        parseElementValue(context, "$ownerPath.array_value.values[$index]")
                    },
                )
            }
            else -> throw ClassFileFormatException(
                "Invalid annotation element_value tag='$tag' at $ownerPath: expected B C D F I J S Z s e c @ or [",
            )
        }
    }

    private inline fun <reified T : ConstantPoolEntry> readConstValueIndex(
        context: AttributeParseContext,
        ownerPath: String,
        tag: Char,
    ): ConstantPoolIndex {
        val index = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            reader = context.reader,
            role = "$ownerPath.const_value_index",
        )
        val entry = constantPoolEntry(context, "$ownerPath.const_value_index", index)
        if (entry !is T) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.const_value_index=$index for tag '$tag': " +
                    "expected ${constantPoolTypeName<T>()} but found ${entry.javaClass.simpleName}",
            )
        }
        return index
    }

    private inline fun <reified T : ConstantPoolEntry> constantPoolTypeName(): String =
        when (T::class) {
            ConstantIntegerEntry::class -> "CONSTANT_Integer_info"
            ConstantDoubleEntry::class -> "CONSTANT_Double_info"
            ConstantFloatEntry::class -> "CONSTANT_Float_info"
            ConstantLongEntry::class -> "CONSTANT_Long_info"
            ConstantUtf8Entry::class -> "CONSTANT_Utf8_info"
            else -> T::class.java.simpleName
        }

    private fun readUtf8Index(
        context: AttributeParseContext,
        role: String,
    ): ConstantPoolIndex {
        val index = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            reader = context.reader,
            role = role,
        )
        val entry = constantPoolEntry(context, role, index)
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $role=$index: expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return index
    }

    private fun constantPoolEntry(
        context: AttributeParseContext,
        role: String,
        index: ConstantPoolIndex,
    ): ConstantPoolEntry =
        try {
            context.constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException("Invalid $role=$index: ${exception.message}")
        }
}
