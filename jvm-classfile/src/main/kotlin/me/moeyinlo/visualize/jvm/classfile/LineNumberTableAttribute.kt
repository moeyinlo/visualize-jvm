package me.moeyinlo.visualize.jvm.classfile

data class LineNumberTableAttribute(
    override val nameIndex: ConstantPoolIndex,
    val entries: List<LineNumberTableEntry>,
) : AttributeInfo

data class LineNumberTableEntry(
    val startPc: Int,
    val lineNumber: Int,
)

object LineNumberTableAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val lineNumberTableLength = context.reader.readU2()
        val entries = List(lineNumberTableLength) {
            LineNumberTableEntry(
                startPc = context.reader.readU2(),
                lineNumber = context.reader.readU2(),
            )
        }
        return LineNumberTableAttribute(
            nameIndex = context.nameIndex,
            entries = entries,
        )
    }
}
