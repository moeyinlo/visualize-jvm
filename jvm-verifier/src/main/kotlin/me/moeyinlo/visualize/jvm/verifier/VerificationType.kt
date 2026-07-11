package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.VerificationTypeInfo as ClassfileVerificationTypeInfo

sealed interface VerificationType {
    val locationCount: Int

    fun isAssignableTo(target: VerificationType): kotlin.Boolean =
        VerificationTypeLattice.isAssignable(source = this, target = target)

    data object Top : VerificationType {
        override val locationCount: Int = 1
    }

    data object OneWord : VerificationType {
        override val locationCount: Int = 1
    }

    data object TwoWord : VerificationType {
        override val locationCount: Int = 2
    }

    data object Reference : VerificationType {
        override val locationCount: Int = 1
    }

    data object UninitializedReference : VerificationType {
        override val locationCount: Int = 1
    }

    data object Byte : VerificationType {
        override val locationCount: Int = 1
    }

    data object Boolean : VerificationType {
        override val locationCount: Int = 1
    }

    data object Char : VerificationType {
        override val locationCount: Int = 1
    }

    data object Short : VerificationType {
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

    data class ArrayOf(val component: VerificationType) : VerificationType {
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

private object VerificationTypeLattice {
    fun isAssignable(source: VerificationType, target: VerificationType): kotlin.Boolean {
        if (source == target) {
            return true
        }
        if (source == VerificationType.Null && (target is VerificationType.ObjectType || target is VerificationType.ArrayOf)) {
            return true
        }
        return directSupertypes(source).any { supertype ->
            isAssignable(source = supertype, target = target)
        }
    }

    private fun directSupertypes(type: VerificationType): List<VerificationType> =
        when (type) {
            VerificationType.Top -> emptyList()
            VerificationType.OneWord -> listOf(VerificationType.Top)
            VerificationType.TwoWord -> listOf(VerificationType.Top)
            VerificationType.Byte,
            VerificationType.Boolean,
            VerificationType.Char,
            VerificationType.Short,
            -> emptyList()
            VerificationType.Integer,
            VerificationType.Float,
            VerificationType.Reference,
            -> listOf(VerificationType.OneWord)
            VerificationType.Long,
            VerificationType.Double,
            -> listOf(VerificationType.TwoWord)
            VerificationType.Null,
            is VerificationType.ObjectType,
            is VerificationType.ArrayOf,
            -> listOf(VerificationType.Reference)
            VerificationType.UninitializedReference -> listOf(VerificationType.Reference)
            VerificationType.UninitializedThis,
            is VerificationType.Uninitialized,
            -> listOf(VerificationType.UninitializedReference)
        }
}
