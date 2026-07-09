package me.moeyinlo.visualize.jvm.oracle

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class AsmOracleException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

data class AsmClassFacts(
    val minorVersion: Int,
    val majorVersion: Int,
    val access: Int,
    val name: String,
    val superName: String?,
    val interfaces: List<String>,
    val constantPoolEntryCount: Int,
    val fields: List<AsmFieldFacts>,
    val methods: List<AsmMethodFacts>,
)

data class AsmFieldFacts(
    val access: Int,
    val name: String,
    val descriptor: String,
    val signature: String?,
    val constantValue: Any?,
)

data class AsmMethodFacts(
    val access: Int,
    val name: String,
    val descriptor: String,
    val signature: String?,
    val exceptions: List<String>,
)

object AsmOracle {
    fun parse(classBytes: ByteArray): AsmClassFacts {
        if (classBytes.size < 8) {
            throw AsmOracleException("ASM failed to parse class bytes (length=${classBytes.size}): input is shorter than a classfile header")
        }

        return try {
            val reader = ClassReader(classBytes)
            val collector = FactCollectingVisitor()
            reader.accept(
                collector,
                ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES,
            )
            collector.toFacts(
                minorVersion = classBytes.u2(4),
                majorVersion = classBytes.u2(6),
                constantPoolEntryCount = reader.itemCount,
            )
        } catch (exception: RuntimeException) {
            throw AsmOracleException(
                "ASM failed to parse class bytes (length=${classBytes.size}): ${exception.message}",
                exception,
            )
        }
    }

    private class FactCollectingVisitor : ClassVisitor(Opcodes.ASM9) {
        private var access: Int? = null
        private var name: String? = null
        private var superName: String? = null
        private var interfaces: List<String> = emptyList()
        private val fields = mutableListOf<AsmFieldFacts>()
        private val methods = mutableListOf<AsmMethodFacts>()

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>,
        ) {
            this.access = access
            this.name = name
            this.superName = superName
            this.interfaces = interfaces.toList()
        }

        override fun visitField(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            value: Any?,
        ): FieldVisitor? {
            fields += AsmFieldFacts(
                access = access,
                name = name,
                descriptor = descriptor,
                signature = signature,
                constantValue = value,
            )
            return null
        }

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor? {
            methods += AsmMethodFacts(
                access = access,
                name = name,
                descriptor = descriptor,
                signature = signature,
                exceptions = exceptions?.toList().orEmpty(),
            )
            return null
        }

        fun toFacts(
            minorVersion: Int,
            majorVersion: Int,
            constantPoolEntryCount: Int,
        ): AsmClassFacts = AsmClassFacts(
            minorVersion = minorVersion,
            majorVersion = majorVersion,
            access = access ?: error("ASM did not visit class header"),
            name = name ?: error("ASM did not visit class name"),
            superName = superName,
            interfaces = interfaces,
            constantPoolEntryCount = constantPoolEntryCount,
            fields = fields.toList(),
            methods = methods.toList(),
        )
    }

    private fun ByteArray.u2(offset: Int): Int =
        ((this[offset].toInt() and 0xFF) shl 8) or (this[offset + 1].toInt() and 0xFF)
}
