package me.moeyinlo.visualize.jvm.classfile

enum class ClassAccessFlag(val mask: Int, val specName: String) {
    Public(0x0001, "ACC_PUBLIC"),
    Final(0x0010, "ACC_FINAL"),
    Super(0x0020, "ACC_SUPER"),
    Interface(0x0200, "ACC_INTERFACE"),
    Abstract(0x0400, "ACC_ABSTRACT"),
    Synthetic(0x1000, "ACC_SYNTHETIC"),
    Annotation(0x2000, "ACC_ANNOTATION"),
    Enum(0x4000, "ACC_ENUM"),
    Module(0x8000, "ACC_MODULE"),
}

enum class ClassFileKind {
    Class,
    Interface,
    AnnotationInterface,
    Module,
}

data class ClassAccessFlags(
    val raw: Int,
    val kind: ClassFileKind,
    val reservedBits: Int,
) {
    fun has(flag: ClassAccessFlag): Boolean = raw and flag.mask != 0
}

object ClassAccessFlagsParser {
    private val assignedMask: Int = ClassAccessFlag.entries.fold(0) { mask, flag -> mask or flag.mask }

    fun parse(reader: ClassFileByteReader): ClassAccessFlags {
        val offset = reader.position
        val raw = reader.readU2()
        validate(raw, reader.source, offset)
        return ClassAccessFlags(
            raw = raw,
            kind = kind(raw),
            reservedBits = raw and assignedMask.inv() and 0xFFFF,
        )
    }

    private fun validate(raw: Int, source: String, offset: Int) {
        if (has(raw, ClassAccessFlag.Module)) {
            if (raw != ClassAccessFlag.Module.mask) {
                fail(source, offset, "ACC_MODULE may be set only when no other flag is set")
            }
            return
        }

        if (has(raw, ClassAccessFlag.Annotation) && !has(raw, ClassAccessFlag.Interface)) {
            fail(source, offset, "ACC_ANNOTATION requires ACC_INTERFACE")
        }

        if (has(raw, ClassAccessFlag.Interface)) {
            if (!has(raw, ClassAccessFlag.Abstract)) {
                fail(source, offset, "ACC_INTERFACE requires ACC_ABSTRACT")
            }

            listOf(
                ClassAccessFlag.Final,
                ClassAccessFlag.Super,
                ClassAccessFlag.Enum,
                ClassAccessFlag.Module,
            ).firstOrNull { has(raw, it) }?.let { flag ->
                fail(source, offset, "ACC_INTERFACE must not be combined with ${flag.specName}")
            }
        } else {
            if (has(raw, ClassAccessFlag.Annotation)) {
                fail(source, offset, "ACC_ANNOTATION is legal only on interfaces")
            }
            if (has(raw, ClassAccessFlag.Final) && has(raw, ClassAccessFlag.Abstract)) {
                fail(source, offset, "ACC_FINAL and ACC_ABSTRACT must not both be set")
            }
        }
    }

    private fun kind(raw: Int): ClassFileKind =
        when {
            has(raw, ClassAccessFlag.Module) -> ClassFileKind.Module
            has(raw, ClassAccessFlag.Annotation) -> ClassFileKind.AnnotationInterface
            has(raw, ClassAccessFlag.Interface) -> ClassFileKind.Interface
            else -> ClassFileKind.Class
        }

    private fun has(raw: Int, flag: ClassAccessFlag): Boolean = raw and flag.mask != 0

    private fun fail(source: String, offset: Int, message: String): Nothing =
        throw ClassFileFormatException("Invalid class access_flags source=$source offset=$offset: $message")
}
