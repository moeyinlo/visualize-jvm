package me.moeyinlo.visualize.jvm.classfile

data class StackMapTableAttribute(
    override val nameIndex: ConstantPoolIndex,
    val entries: List<StackMapFrame>,
) : AttributeInfo

sealed interface StackMapFrame {
    val frameType: Int
    val offsetDelta: Int
}

data class SameStackMapFrame(
    override val frameType: Int,
) : StackMapFrame {
    override val offsetDelta: Int = frameType
}

data class SameLocalsOneStackItemFrame(
    override val frameType: Int,
    val stack: VerificationTypeInfo,
) : StackMapFrame {
    override val offsetDelta: Int = frameType - 64
}

data class SameLocalsOneStackItemFrameExtended(
    override val offsetDelta: Int,
    val stack: VerificationTypeInfo,
) : StackMapFrame {
    override val frameType: Int = 247
}

data class ChopStackMapFrame(
    override val frameType: Int,
    override val offsetDelta: Int,
) : StackMapFrame {
    val choppedLocals: Int = 251 - frameType
}

data class SameStackMapFrameExtended(
    override val offsetDelta: Int,
) : StackMapFrame {
    override val frameType: Int = 251
}

data class AppendStackMapFrame(
    override val frameType: Int,
    override val offsetDelta: Int,
    val locals: List<VerificationTypeInfo>,
) : StackMapFrame {
    val appendedLocals: Int = frameType - 251
}

data class FullStackMapFrame(
    override val offsetDelta: Int,
    val locals: List<VerificationTypeInfo>,
    val stack: List<VerificationTypeInfo>,
) : StackMapFrame {
    override val frameType: Int = 255
}

sealed interface VerificationTypeInfo {
    val tag: Int

    data object Top : VerificationTypeInfo {
        override val tag: Int = 0
    }

    data object Integer : VerificationTypeInfo {
        override val tag: Int = 1
    }

    data object Float : VerificationTypeInfo {
        override val tag: Int = 2
    }

    data object Double : VerificationTypeInfo {
        override val tag: Int = 3
    }

    data object Long : VerificationTypeInfo {
        override val tag: Int = 4
    }

    data object Null : VerificationTypeInfo {
        override val tag: Int = 5
    }

    data object UninitializedThis : VerificationTypeInfo {
        override val tag: Int = 6
    }

    data class ObjectVariable(val cpoolIndex: ConstantPoolIndex) : VerificationTypeInfo {
        override val tag: Int = 7
    }

    data class UninitializedVariable(val offset: Int) : VerificationTypeInfo {
        override val tag: Int = 8
    }
}

object StackMapTableAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        if (context.majorVersion < Java6MajorVersion) {
            throw ClassFileFormatException(
                "Invalid StackMapTable attribute at ${context.ownerPath}: " +
                    "major_version=${context.majorVersion} must be at least $Java6MajorVersion",
            )
        }
        val numberOfEntries = context.reader.readU2()
        val entries = List(numberOfEntries) { index ->
            parseFrame(context, "${context.ownerPath}.entries[$index]")
        }
        return StackMapTableAttribute(
            nameIndex = context.nameIndex,
            entries = entries,
        )
    }

    private fun parseFrame(
        context: AttributeParseContext,
        ownerPath: String,
    ): StackMapFrame {
        val frameType = context.reader.readU1()
        return when (frameType) {
            in 0..63 -> SameStackMapFrame(frameType)
            in 64..127 -> SameLocalsOneStackItemFrame(
                frameType = frameType,
                stack = parseVerificationType(context, "$ownerPath.stack[0]"),
            )
            in 128..246 -> throw ClassFileFormatException(
                "Invalid StackMapTable frame_type=$frameType at $ownerPath: reserved for future use",
            )
            247 -> SameLocalsOneStackItemFrameExtended(
                offsetDelta = context.reader.readU2(),
                stack = parseVerificationType(context, "$ownerPath.stack[0]"),
            )
            in 248..250 -> ChopStackMapFrame(
                frameType = frameType,
                offsetDelta = context.reader.readU2(),
            )
            251 -> SameStackMapFrameExtended(
                offsetDelta = context.reader.readU2(),
            )
            in 252..254 -> {
                val offsetDelta = context.reader.readU2()
                AppendStackMapFrame(
                    frameType = frameType,
                    offsetDelta = offsetDelta,
                    locals = List(frameType - 251) { index ->
                        parseVerificationType(context, "$ownerPath.locals[$index]")
                    },
                )
            }
            255 -> {
                val offsetDelta = context.reader.readU2()
                val locals = List(context.reader.readU2()) { index ->
                    parseVerificationType(context, "$ownerPath.locals[$index]")
                }
                val stack = List(context.reader.readU2()) { index ->
                    parseVerificationType(context, "$ownerPath.stack[$index]")
                }
                FullStackMapFrame(
                    offsetDelta = offsetDelta,
                    locals = locals,
                    stack = stack,
                )
            }
            else -> error("u1 frame_type is outside 0..255: $frameType")
        }
    }

    private fun parseVerificationType(
        context: AttributeParseContext,
        ownerPath: String,
    ): VerificationTypeInfo {
        val tag = context.reader.readU1()
        return when (tag) {
            0 -> VerificationTypeInfo.Top
            1 -> VerificationTypeInfo.Integer
            2 -> VerificationTypeInfo.Float
            3 -> VerificationTypeInfo.Double
            4 -> VerificationTypeInfo.Long
            5 -> VerificationTypeInfo.Null
            6 -> VerificationTypeInfo.UninitializedThis
            7 -> VerificationTypeInfo.ObjectVariable(readClassConstantIndex(context, ownerPath))
            8 -> VerificationTypeInfo.UninitializedVariable(context.reader.readU2())
            else -> throw ClassFileFormatException(
                "Invalid StackMapTable verification_type tag=$tag at $ownerPath: expected 0..8",
            )
        }
    }

    private fun readClassConstantIndex(
        context: AttributeParseContext,
        ownerPath: String,
    ): ConstantPoolIndex {
        val index = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            reader = context.reader,
            role = "$ownerPath.cpool_index",
        )
        val entry = try {
            context.constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.cpool_index=$index: ${exception.message}",
            )
        }
        if (entry !is ConstantClassEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.cpool_index=$index: expected CONSTANT_Class_info " +
                    "but found ${entry.javaClass.simpleName}",
            )
        }
        return index
    }
}

private const val Java6MajorVersion = 50
