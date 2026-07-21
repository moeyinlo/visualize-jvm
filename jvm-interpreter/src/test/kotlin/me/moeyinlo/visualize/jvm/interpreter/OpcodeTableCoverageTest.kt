package me.moeyinlo.visualize.jvm.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals

class OpcodeTableCoverageTest {
    @Test
    fun `opcode table matches the JVMS opcode table shape`() {
        val actual = OpcodeTable.entries.associate { metadata ->
            metadata.opcode to ExpectedOpcode(
                mnemonic = metadata.mnemonic,
                format = metadata.format,
                fixedLength = metadata.fixedLength,
            )
        }

        assertEquals(256, OpcodeTable.entries.size)
        assertEquals((0x00..0xFF).toList(), OpcodeTable.entries.map(OpcodeMetadata::opcode))
        assertEquals(expectedOpcodes(), actual)
    }

    private fun expectedOpcodes(): Map<Int, ExpectedOpcode> =
        buildMap {
            fixedOpcodeRows.lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .forEach { row ->
                    val columns = row.split(Regex("\\s+"))
                    fixed(
                        opcode = columns[0].toInt(16),
                        mnemonic = columns[1],
                        fixedLength = columns[2].toInt(),
                    )
                }
            variable(0xAA, "tableswitch", OpcodeFormat.TableSwitch)
            variable(0xAB, "lookupswitch", OpcodeFormat.LookupSwitch)
            variable(0xC4, "wide", OpcodeFormat.Wide)
            reserved(0xCA, "breakpoint")
            for (opcode in 0xCB..0xFD) {
                reserved(opcode, "reserved_0x${opcode.toString(16).padStart(2, '0')}")
            }
            reserved(0xFE, "impdep1")
            reserved(0xFF, "impdep2")
        }

    private fun MutableMap<Int, ExpectedOpcode>.fixed(
        opcode: Int,
        mnemonic: String,
        fixedLength: Int,
    ) {
        put(opcode, ExpectedOpcode(mnemonic, OpcodeFormat.Fixed, fixedLength))
    }

    private fun MutableMap<Int, ExpectedOpcode>.variable(
        opcode: Int,
        mnemonic: String,
        format: OpcodeFormat,
    ) {
        put(opcode, ExpectedOpcode(mnemonic, format, fixedLength = null))
    }

    private fun MutableMap<Int, ExpectedOpcode>.reserved(
        opcode: Int,
        mnemonic: String,
    ) {
        put(opcode, ExpectedOpcode(mnemonic, OpcodeFormat.Reserved, fixedLength = 1))
    }

    private data class ExpectedOpcode(
        val mnemonic: String,
        val format: OpcodeFormat,
        val fixedLength: Int?,
    )

    private companion object {
        private val fixedOpcodeRows = """
            00 nop 1
            01 aconst_null 1
            02 iconst_m1 1
            03 iconst_0 1
            04 iconst_1 1
            05 iconst_2 1
            06 iconst_3 1
            07 iconst_4 1
            08 iconst_5 1
            09 lconst_0 1
            0A lconst_1 1
            0B fconst_0 1
            0C fconst_1 1
            0D fconst_2 1
            0E dconst_0 1
            0F dconst_1 1
            10 bipush 2
            11 sipush 3
            12 ldc 2
            13 ldc_w 3
            14 ldc2_w 3
            15 iload 2
            16 lload 2
            17 fload 2
            18 dload 2
            19 aload 2
            1A iload_0 1
            1B iload_1 1
            1C iload_2 1
            1D iload_3 1
            1E lload_0 1
            1F lload_1 1
            20 lload_2 1
            21 lload_3 1
            22 fload_0 1
            23 fload_1 1
            24 fload_2 1
            25 fload_3 1
            26 dload_0 1
            27 dload_1 1
            28 dload_2 1
            29 dload_3 1
            2A aload_0 1
            2B aload_1 1
            2C aload_2 1
            2D aload_3 1
            2E iaload 1
            2F laload 1
            30 faload 1
            31 daload 1
            32 aaload 1
            33 baload 1
            34 caload 1
            35 saload 1
            36 istore 2
            37 lstore 2
            38 fstore 2
            39 dstore 2
            3A astore 2
            3B istore_0 1
            3C istore_1 1
            3D istore_2 1
            3E istore_3 1
            3F lstore_0 1
            40 lstore_1 1
            41 lstore_2 1
            42 lstore_3 1
            43 fstore_0 1
            44 fstore_1 1
            45 fstore_2 1
            46 fstore_3 1
            47 dstore_0 1
            48 dstore_1 1
            49 dstore_2 1
            4A dstore_3 1
            4B astore_0 1
            4C astore_1 1
            4D astore_2 1
            4E astore_3 1
            4F iastore 1
            50 lastore 1
            51 fastore 1
            52 dastore 1
            53 aastore 1
            54 bastore 1
            55 castore 1
            56 sastore 1
            57 pop 1
            58 pop2 1
            59 dup 1
            5A dup_x1 1
            5B dup_x2 1
            5C dup2 1
            5D dup2_x1 1
            5E dup2_x2 1
            5F swap 1
            60 iadd 1
            61 ladd 1
            62 fadd 1
            63 dadd 1
            64 isub 1
            65 lsub 1
            66 fsub 1
            67 dsub 1
            68 imul 1
            69 lmul 1
            6A fmul 1
            6B dmul 1
            6C idiv 1
            6D ldiv 1
            6E fdiv 1
            6F ddiv 1
            70 irem 1
            71 lrem 1
            72 frem 1
            73 drem 1
            74 ineg 1
            75 lneg 1
            76 fneg 1
            77 dneg 1
            78 ishl 1
            79 lshl 1
            7A ishr 1
            7B lshr 1
            7C iushr 1
            7D lushr 1
            7E iand 1
            7F land 1
            80 ior 1
            81 lor 1
            82 ixor 1
            83 lxor 1
            84 iinc 3
            85 i2l 1
            86 i2f 1
            87 i2d 1
            88 l2i 1
            89 l2f 1
            8A l2d 1
            8B f2i 1
            8C f2l 1
            8D f2d 1
            8E d2i 1
            8F d2l 1
            90 d2f 1
            91 i2b 1
            92 i2c 1
            93 i2s 1
            94 lcmp 1
            95 fcmpl 1
            96 fcmpg 1
            97 dcmpl 1
            98 dcmpg 1
            99 ifeq 3
            9A ifne 3
            9B iflt 3
            9C ifge 3
            9D ifgt 3
            9E ifle 3
            9F if_icmpeq 3
            A0 if_icmpne 3
            A1 if_icmplt 3
            A2 if_icmpge 3
            A3 if_icmpgt 3
            A4 if_icmple 3
            A5 if_acmpeq 3
            A6 if_acmpne 3
            A7 goto 3
            A8 jsr 3
            A9 ret 2
            AC ireturn 1
            AD lreturn 1
            AE freturn 1
            AF dreturn 1
            B0 areturn 1
            B1 return 1
            B2 getstatic 3
            B3 putstatic 3
            B4 getfield 3
            B5 putfield 3
            B6 invokevirtual 3
            B7 invokespecial 3
            B8 invokestatic 3
            B9 invokeinterface 5
            BA invokedynamic 5
            BB new 3
            BC newarray 2
            BD anewarray 3
            BE arraylength 1
            BF athrow 1
            C0 checkcast 3
            C1 instanceof 3
            C2 monitorenter 1
            C3 monitorexit 1
            C5 multianewarray 4
            C6 ifnull 3
            C7 ifnonnull 3
            C8 goto_w 5
            C9 jsr_w 5
        """.trimIndent()
    }
}
