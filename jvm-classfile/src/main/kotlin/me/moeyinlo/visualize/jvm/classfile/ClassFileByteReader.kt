package me.moeyinlo.visualize.jvm.classfile

class ClassFileReadException(
    val source: String,
    val offset: Int,
    val need: Int,
    val remaining: Int,
) : RuntimeException("Unexpected end of classfile source=$source offset=$offset need=$need remaining=$remaining")

class ClassFileByteReader(
    bytes: ByteArray,
    val source: String = "<memory>",
    private val baseOffset: Int = 0,
) {
    private val data = bytes.copyOf()

    init {
        require(baseOffset >= 0) { "Base offset must be non-negative: $baseOffset" }
    }

    var position: Int = 0
        private set

    val currentOffset: Int
        get() = baseOffset + position

    val size: Int
        get() = data.size

    val remaining: Int
        get() = size - position

    fun readU1(): Int {
        requireAvailable(1)
        val value = data[position].toInt() and 0xFF
        position += 1
        return value
    }

    fun readU2(): Int {
        requireAvailable(2)
        val value = ((data[position].toInt() and 0xFF) shl 8) or
            (data[position + 1].toInt() and 0xFF)
        position += 2
        return value
    }

    fun readU4(): Long {
        requireAvailable(4)
        val value = ((data[position].toLong() and 0xFFL) shl 24) or
            ((data[position + 1].toLong() and 0xFFL) shl 16) or
            ((data[position + 2].toLong() and 0xFFL) shl 8) or
            (data[position + 3].toLong() and 0xFFL)
        position += 4
        return value
    }

    fun readSlice(length: Int): ByteArray {
        require(length >= 0) { "Slice length must be non-negative: $length" }
        requireAvailable(length)
        val slice = data.copyOfRange(position, position + length)
        position += length
        return slice
    }

    private fun requireAvailable(length: Int) {
        require(length >= 0) { "Read length must be non-negative: $length" }
        if (remaining < length) {
            throw ClassFileReadException(
                source = source,
                offset = currentOffset,
                need = length,
                remaining = remaining,
            )
        }
    }
}
