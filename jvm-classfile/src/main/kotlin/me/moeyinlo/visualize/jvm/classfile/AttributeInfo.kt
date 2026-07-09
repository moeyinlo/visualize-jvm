package me.moeyinlo.visualize.jvm.classfile

interface AttributeInfo {
    val nameIndex: ConstantPoolIndex
}

class UnknownAttributeInfo(
    override val nameIndex: ConstantPoolIndex,
    val name: String,
    info: ByteArray,
) : AttributeInfo {
    private val infoBytes = info.copyOf()

    val info: ByteArray
        get() = infoBytes.copyOf()
}

fun interface AttributeBodyParser {
    fun parse(context: AttributeParseContext): AttributeInfo
}

class AttributeParseContext(
    val nameIndex: ConstantPoolIndex,
    val name: String,
    val ownerPath: String,
    val source: String,
    val infoStartOffset: Int,
    val constantPool: ConstantPool,
    val registry: AttributeParserRegistry,
    info: ByteArray,
) {
    private val infoBytes = info.copyOf()

    val length: Int
        get() = infoBytes.size

    val info: ByteArray
        get() = infoBytes.copyOf()

    val reader: ClassFileByteReader = ClassFileByteReader(
        bytes = infoBytes,
        source = source,
        baseOffset = infoStartOffset,
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
        val infoStartOffset = reader.currentOffset
        val info = try {
            reader.readSlice(attributeLength.toInt())
        } catch (exception: ClassFileReadException) {
            throw ClassFileFormatException(
                "Invalid attribute info source=${exception.source} path=$ownerPath " +
                    "offset=${exception.offset}: ${exception.message}",
                exception,
            )
        }
        val parser = registry.parserFor(name)
            ?: return UnknownAttributeInfo(
                nameIndex = nameIndex,
                name = name,
                info = info,
            )
        val context = AttributeParseContext(
            nameIndex = nameIndex,
            name = name,
            ownerPath = ownerPath,
            source = reader.source,
            infoStartOffset = infoStartOffset,
            constantPool = constantPool,
            registry = registry,
            info = info,
        )
        val attribute = parseAttributeBody(parser, context)
        if (context.reader.remaining != 0) {
            throw ClassFileFormatException(
                "Attribute parser for $name left ${context.reader.remaining} unconsumed byte(s) " +
                    "source=${reader.source} path=$ownerPath offset=${context.reader.currentOffset}",
            )
        }
        return attribute
    }

    private fun parseAttributeBody(
        parser: AttributeBodyParser,
        context: AttributeParseContext,
    ): AttributeInfo =
        try {
            parser.parse(context)
        } catch (exception: ClassFileReadException) {
            throw ClassFileFormatException(
                "Invalid attribute body source=${exception.source} path=${context.ownerPath} " +
                    "offset=${exception.offset}: ${exception.message}",
                exception,
            )
        } catch (exception: ClassFileFormatException) {
            if (exception.hasSourcePathOffset()) {
                throw exception
            }
            throw ClassFileFormatException(
                "Invalid attribute body source=${context.source} path=${context.ownerPath} " +
                    "offset=${context.reader.currentOffset}: ${exception.message}",
                exception,
            )
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

    private fun ClassFileFormatException.hasSourcePathOffset(): Boolean {
        val text = message.orEmpty()
        return text.contains("source=") && text.contains("path=") && text.contains("offset=")
    }
}
