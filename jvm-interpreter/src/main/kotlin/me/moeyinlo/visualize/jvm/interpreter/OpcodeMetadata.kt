package me.moeyinlo.visualize.jvm.interpreter

data class OpcodeMetadata(
    val opcode: Int,
    val mnemonic: String,
    val format: OpcodeFormat,
    val fixedLength: Int?,
) {
    init {
        require(opcode in 0..0xFF) { "Opcode must be an unsigned byte: $opcode" }
        require(mnemonic.isNotBlank()) { "Opcode mnemonic must not be blank" }
        if (format == OpcodeFormat.Fixed || format == OpcodeFormat.Reserved) {
            require(fixedLength != null && fixedLength >= 1) {
                "Fixed and reserved opcode metadata must provide a positive fixed length"
            }
        } else {
            require(fixedLength == null) { "Variable opcode metadata must not provide a fixed length" }
        }
    }
}

enum class OpcodeFormat {
    Fixed,
    TableSwitch,
    LookupSwitch,
    Wide,
    Reserved,
}

object OpcodeTable {
    val entries: List<OpcodeMetadata> = buildEntries()

    fun metadata(opcode: Int): OpcodeMetadata {
        require(opcode in 0..0xFF) { "Opcode must be an unsigned byte: $opcode" }
        return entries[opcode]
    }

    fun metadata(opcode: Byte): OpcodeMetadata = metadata(opcode.toInt() and 0xFF)

    private fun buildEntries(): List<OpcodeMetadata> {
        val known = fixedOpcodes() + variableOpcodes() + namedReservedOpcodes()
        val byOpcode = known.associateBy { metadata -> metadata.opcode }
        require(byOpcode.size == known.size) { "Duplicate opcode metadata entries" }
        return (0..0xFF).map { opcode ->
            byOpcode[opcode] ?: OpcodeMetadata(
                opcode = opcode,
                mnemonic = "reserved_0x${opcode.toString(16).padStart(2, '0')}",
                format = OpcodeFormat.Reserved,
                fixedLength = 1,
            )
        }
    }

    private fun fixedOpcodes(): List<OpcodeMetadata> = listOf(
        fixed(0x00, "nop", 1),
        fixed(0x01, "aconst_null", 1),
        fixed(0x02, "iconst_m1", 1),
        fixed(0x03, "iconst_0", 1),
        fixed(0x04, "iconst_1", 1),
        fixed(0x05, "iconst_2", 1),
        fixed(0x06, "iconst_3", 1),
        fixed(0x07, "iconst_4", 1),
        fixed(0x08, "iconst_5", 1),
        fixed(0x09, "lconst_0", 1),
        fixed(0x0A, "lconst_1", 1),
        fixed(0x0B, "fconst_0", 1),
        fixed(0x0C, "fconst_1", 1),
        fixed(0x0D, "fconst_2", 1),
        fixed(0x0E, "dconst_0", 1),
        fixed(0x0F, "dconst_1", 1),
        fixed(0x10, "bipush", 2),
        fixed(0x11, "sipush", 3),
        fixed(0x12, "ldc", 2),
        fixed(0x13, "ldc_w", 3),
        fixed(0x14, "ldc2_w", 3),
        fixed(0x15, "iload", 2),
        fixed(0x16, "lload", 2),
        fixed(0x17, "fload", 2),
        fixed(0x18, "dload", 2),
        fixed(0x19, "aload", 2),
        fixed(0x1A, "iload_0", 1),
        fixed(0x1B, "iload_1", 1),
        fixed(0x1C, "iload_2", 1),
        fixed(0x1D, "iload_3", 1),
        fixed(0x1E, "lload_0", 1),
        fixed(0x1F, "lload_1", 1),
        fixed(0x20, "lload_2", 1),
        fixed(0x21, "lload_3", 1),
        fixed(0x22, "fload_0", 1),
        fixed(0x23, "fload_1", 1),
        fixed(0x24, "fload_2", 1),
        fixed(0x25, "fload_3", 1),
        fixed(0x26, "dload_0", 1),
        fixed(0x27, "dload_1", 1),
        fixed(0x28, "dload_2", 1),
        fixed(0x29, "dload_3", 1),
        fixed(0x2A, "aload_0", 1),
        fixed(0x2B, "aload_1", 1),
        fixed(0x2C, "aload_2", 1),
        fixed(0x2D, "aload_3", 1),
        fixed(0x2E, "iaload", 1),
        fixed(0x2F, "laload", 1),
        fixed(0x30, "faload", 1),
        fixed(0x31, "daload", 1),
        fixed(0x32, "aaload", 1),
        fixed(0x33, "baload", 1),
        fixed(0x34, "caload", 1),
        fixed(0x35, "saload", 1),
        fixed(0x36, "istore", 2),
        fixed(0x37, "lstore", 2),
        fixed(0x38, "fstore", 2),
        fixed(0x39, "dstore", 2),
        fixed(0x3A, "astore", 2),
        fixed(0x3B, "istore_0", 1),
        fixed(0x3C, "istore_1", 1),
        fixed(0x3D, "istore_2", 1),
        fixed(0x3E, "istore_3", 1),
        fixed(0x3F, "lstore_0", 1),
        fixed(0x40, "lstore_1", 1),
        fixed(0x41, "lstore_2", 1),
        fixed(0x42, "lstore_3", 1),
        fixed(0x43, "fstore_0", 1),
        fixed(0x44, "fstore_1", 1),
        fixed(0x45, "fstore_2", 1),
        fixed(0x46, "fstore_3", 1),
        fixed(0x47, "dstore_0", 1),
        fixed(0x48, "dstore_1", 1),
        fixed(0x49, "dstore_2", 1),
        fixed(0x4A, "dstore_3", 1),
        fixed(0x4B, "astore_0", 1),
        fixed(0x4C, "astore_1", 1),
        fixed(0x4D, "astore_2", 1),
        fixed(0x4E, "astore_3", 1),
        fixed(0x4F, "iastore", 1),
        fixed(0x50, "lastore", 1),
        fixed(0x51, "fastore", 1),
        fixed(0x52, "dastore", 1),
        fixed(0x53, "aastore", 1),
        fixed(0x54, "bastore", 1),
        fixed(0x55, "castore", 1),
        fixed(0x56, "sastore", 1),
        fixed(0x57, "pop", 1),
        fixed(0x58, "pop2", 1),
        fixed(0x59, "dup", 1),
        fixed(0x5A, "dup_x1", 1),
        fixed(0x5B, "dup_x2", 1),
        fixed(0x5C, "dup2", 1),
        fixed(0x5D, "dup2_x1", 1),
        fixed(0x5E, "dup2_x2", 1),
        fixed(0x5F, "swap", 1),
        fixed(0x60, "iadd", 1),
        fixed(0x61, "ladd", 1),
        fixed(0x62, "fadd", 1),
        fixed(0x63, "dadd", 1),
        fixed(0x64, "isub", 1),
        fixed(0x65, "lsub", 1),
        fixed(0x66, "fsub", 1),
        fixed(0x67, "dsub", 1),
        fixed(0x68, "imul", 1),
        fixed(0x69, "lmul", 1),
        fixed(0x6A, "fmul", 1),
        fixed(0x6B, "dmul", 1),
        fixed(0x6C, "idiv", 1),
        fixed(0x6D, "ldiv", 1),
        fixed(0x6E, "fdiv", 1),
        fixed(0x6F, "ddiv", 1),
        fixed(0x70, "irem", 1),
        fixed(0x71, "lrem", 1),
        fixed(0x72, "frem", 1),
        fixed(0x73, "drem", 1),
        fixed(0x74, "ineg", 1),
        fixed(0x75, "lneg", 1),
        fixed(0x76, "fneg", 1),
        fixed(0x77, "dneg", 1),
        fixed(0x78, "ishl", 1),
        fixed(0x79, "lshl", 1),
        fixed(0x7A, "ishr", 1),
        fixed(0x7B, "lshr", 1),
        fixed(0x7C, "iushr", 1),
        fixed(0x7D, "lushr", 1),
        fixed(0x7E, "iand", 1),
        fixed(0x7F, "land", 1),
        fixed(0x80, "ior", 1),
        fixed(0x81, "lor", 1),
        fixed(0x82, "ixor", 1),
        fixed(0x83, "lxor", 1),
        fixed(0x84, "iinc", 3),
        fixed(0x85, "i2l", 1),
        fixed(0x86, "i2f", 1),
        fixed(0x87, "i2d", 1),
        fixed(0x88, "l2i", 1),
        fixed(0x89, "l2f", 1),
        fixed(0x8A, "l2d", 1),
        fixed(0x8B, "f2i", 1),
        fixed(0x8C, "f2l", 1),
        fixed(0x8D, "f2d", 1),
        fixed(0x8E, "d2i", 1),
        fixed(0x8F, "d2l", 1),
        fixed(0x90, "d2f", 1),
        fixed(0x91, "i2b", 1),
        fixed(0x92, "i2c", 1),
        fixed(0x93, "i2s", 1),
        fixed(0x94, "lcmp", 1),
        fixed(0x95, "fcmpl", 1),
        fixed(0x96, "fcmpg", 1),
        fixed(0x97, "dcmpl", 1),
        fixed(0x98, "dcmpg", 1),
        fixed(0x99, "ifeq", 3),
        fixed(0x9A, "ifne", 3),
        fixed(0x9B, "iflt", 3),
        fixed(0x9C, "ifge", 3),
        fixed(0x9D, "ifgt", 3),
        fixed(0x9E, "ifle", 3),
        fixed(0x9F, "if_icmpeq", 3),
        fixed(0xA0, "if_icmpne", 3),
        fixed(0xA1, "if_icmplt", 3),
        fixed(0xA2, "if_icmpge", 3),
        fixed(0xA3, "if_icmpgt", 3),
        fixed(0xA4, "if_icmple", 3),
        fixed(0xA5, "if_acmpeq", 3),
        fixed(0xA6, "if_acmpne", 3),
        fixed(0xA7, "goto", 3),
        fixed(0xA8, "jsr", 3),
        fixed(0xA9, "ret", 2),
        fixed(0xAC, "ireturn", 1),
        fixed(0xAD, "lreturn", 1),
        fixed(0xAE, "freturn", 1),
        fixed(0xAF, "dreturn", 1),
        fixed(0xB0, "areturn", 1),
        fixed(0xB1, "return", 1),
        fixed(0xB2, "getstatic", 3),
        fixed(0xB3, "putstatic", 3),
        fixed(0xB4, "getfield", 3),
        fixed(0xB5, "putfield", 3),
        fixed(0xB6, "invokevirtual", 3),
        fixed(0xB7, "invokespecial", 3),
        fixed(0xB8, "invokestatic", 3),
        fixed(0xB9, "invokeinterface", 5),
        fixed(0xBA, "invokedynamic", 5),
        fixed(0xBB, "new", 3),
        fixed(0xBC, "newarray", 2),
        fixed(0xBD, "anewarray", 3),
        fixed(0xBE, "arraylength", 1),
        fixed(0xBF, "athrow", 1),
        fixed(0xC0, "checkcast", 3),
        fixed(0xC1, "instanceof", 3),
        fixed(0xC2, "monitorenter", 1),
        fixed(0xC3, "monitorexit", 1),
        fixed(0xC5, "multianewarray", 4),
        fixed(0xC6, "ifnull", 3),
        fixed(0xC7, "ifnonnull", 3),
        fixed(0xC8, "goto_w", 5),
        fixed(0xC9, "jsr_w", 5),
    )

    private fun variableOpcodes(): List<OpcodeMetadata> = listOf(
        OpcodeMetadata(opcode = 0xAA, mnemonic = "tableswitch", format = OpcodeFormat.TableSwitch, fixedLength = null),
        OpcodeMetadata(opcode = 0xAB, mnemonic = "lookupswitch", format = OpcodeFormat.LookupSwitch, fixedLength = null),
        OpcodeMetadata(opcode = 0xC4, mnemonic = "wide", format = OpcodeFormat.Wide, fixedLength = null),
    )

    private fun namedReservedOpcodes(): List<OpcodeMetadata> = listOf(
        reserved(0xCA, "breakpoint"),
        reserved(0xFE, "impdep1"),
        reserved(0xFF, "impdep2"),
    )

    private fun fixed(opcode: Int, mnemonic: String, fixedLength: Int): OpcodeMetadata =
        OpcodeMetadata(opcode = opcode, mnemonic = mnemonic, format = OpcodeFormat.Fixed, fixedLength = fixedLength)

    private fun reserved(opcode: Int, mnemonic: String): OpcodeMetadata =
        OpcodeMetadata(opcode = opcode, mnemonic = mnemonic, format = OpcodeFormat.Reserved, fixedLength = 1)
}
