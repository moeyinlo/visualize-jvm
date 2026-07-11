package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.VerificationTypeInfo as ClassfileVerificationTypeInfo

sealed interface VerificationType {
    val locationCount: Int

    data object Top : VerificationType {
        override val locationCount: Int = 1
    }

    data object Integer : VerificationType {
        override val locationCount: Int = 1
    }

    data object Float : VerificationType {
        override val locationCount: Int = 1
    }

    data object Long : VerificationType {
        override val locationCount: Int = 2
    }

    data object Double : VerificationType {
        override val locationCount: Int = 2
    }

    data object Null : VerificationType {
        override val locationCount: Int = 1
    }

    data object UninitializedThis : VerificationType {
        override val locationCount: Int = 1
    }

    data class ObjectType(val constantPoolIndex: ConstantPoolIndex) : VerificationType {
        override val locationCount: Int = 1
    }

    data class Uninitialized(val offset: Int) : VerificationType {
        override val locationCount: Int = 1
    }

    companion object {
        fun fromClassfile(type: ClassfileVerificationTypeInfo): VerificationType =
            when (type) {
                ClassfileVerificationTypeInfo.Top -> Top
                ClassfileVerificationTypeInfo.Integer -> Integer
                ClassfileVerificationTypeInfo.Float -> Float
                ClassfileVerificationTypeInfo.Long -> Long
                ClassfileVerificationTypeInfo.Double -> Double
                ClassfileVerificationTypeInfo.Null -> Null
                ClassfileVerificationTypeInfo.UninitializedThis -> UninitializedThis
                is ClassfileVerificationTypeInfo.ObjectVariable -> ObjectType(type.cpoolIndex)
                is ClassfileVerificationTypeInfo.UninitializedVariable -> Uninitialized(type.offset)
            }
    }
}
