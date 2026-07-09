package me.moeyinlo.visualize.jvm.classfile

object ConstantPoolParser {
    fun parse(reader: ClassFileByteReader): ConstantPool {
        val constantPoolCountOffset = reader.position
        val constantPoolCount = reader.readU2()
        if (constantPoolCount < 1) {
            throw ClassFileFormatException(
                "Invalid constant_pool_count=$constantPoolCount source=${reader.source} offset=$constantPoolCountOffset",
            )
        }

        val entries = mutableListOf<ConstantPoolEntry>()
        var index = 1
        while (index < constantPoolCount) {
            val entry = ConstantPoolEntryParser.parseEntry(reader)
            entries += entry
            index += if (entry.occupiesTwoSlots) 2 else 1
            if (index > constantPoolCount) {
                throw ClassFileFormatException(
                    "Constant pool entry #${index - 2} consumes two slots beyond " +
                        "constant_pool_count=$constantPoolCount source=${reader.source}",
                )
            }
        }

        return ConstantPool.fromEntries(entries)
    }
}
