package me.moeyinlo.visualize.jvm.classfile

interface AttributeInfo {
    val nameIndex: ConstantPoolIndex
}

fun interface AttributeBodyParser {
    fun parse(context: AttributeParseContext): AttributeInfo
}

class AttributeParseContext(
    val nameIndex: ConstantPoolIndex,
    val name: String,
    val ownerPath: String,
    val constantPool: ConstantPool,
    info: ByteArray,
) {
    private val infoBytes = info.copyOf()

    val info: ByteArray
        get() = infoBytes.copyOf()

    val reader: ClassFileByteReader = ClassFileByteReader(
        bytes = infoBytes,
        source = "$ownerPath.$name.info",
    )
}

class AttributeParserRegistry private constructor(
    private val parsersByName: Map<String, AttributeBodyParser>,
) {
    fun parserFor(name: String): AttributeBodyParser? = parsersByName[name]

    companion object {
        val Empty: AttributeParserRegistry = AttributeParserRegistry(emptyMap())

        fun of(vararg parsers: Pair<String, AttributeBodyParser>): AttributeParserRegistry {
            val names = mutableSetOf<String>()
            parsers.forEach { (name, _) ->
                require(names.add(name)) { "Duplicate attribute parser for $name" }
            }
            return AttributeParserRegistry(parsers.toMap())
        }
    }
}

object AttributeInfoParser {
    fun parseAttributes(
        reader: ClassFileByteReader,
        constantPool: ConstantPool,
        registry: AttributeParserRegistry,
        ownerPath: String,
    ): List<AttributeInfo> {
        val attributesCount = reader.readU2()
        return List(attributesCount) { index ->
            parseAttribute(
                reader = reader,
                constantPool = constantPool,
                registry = registry,
                ownerPath = "$ownerPath.attributes[$index]",
            )
        }
    }

    private fun parseAttribute(
        reader: ClassFileByteReader,
        constantPool: ConstantPool,
        registry: AttributeParserRegistry,
        ownerPath: String,
    ): AttributeInfo {
        val nameIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(reader, "$ownerPath.attribute_name_index")
        val attributeLength = reader.readU4()
        if (attributeLength > Int.MAX_VALUE) {
            throw ClassFileFormatException(
                "Attribute too large source=${reader.source} $ownerPath.attribute_length=$attributeLength",
            )
        }

        val name = resolveAttributeName(
            constantPool = constantPool,
            nameIndex = nameIndex,
            source = reader.source,
            ownerPath = ownerPath,
        )
        val info = reader.readSlice(attributeLength.toInt())
        val parser = registry.parserFor(name)
            ?: throw ClassFileFormatException("No parser registered for attribute $name source=${reader.source} $ownerPath")
        val context = AttributeParseContext(
            nameIndex = nameIndex,
            name = name,
            ownerPath = ownerPath,
            constantPool = constantPool,
            info = info,
        )
        val attribute = parser.parse(context)
        if (context.reader.remaining != 0) {
            throw ClassFileFormatException(
                "Attribute parser for $name left ${context.reader.remaining} unconsumed byte(s) " +
                    "source=${reader.source} $ownerPath",
            )
        }
        return attribute
    }

    private fun resolveAttributeName(
        constantPool: ConstantPool,
        nameIndex: ConstantPoolIndex,
        source: String,
        ownerPath: String,
    ): String {
        val entry = try {
            constantPool[nameIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.attribute_name_index=$nameIndex source=$source: ${exception.message}",
            )
        }
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.attribute_name_index=$nameIndex source=$source: " +
                    "expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return entry.value
    }
}
