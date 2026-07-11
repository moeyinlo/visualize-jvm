package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.VerificationTypeInfo as ClassfileVerificationTypeInfo

sealed interface VerificationType {
    val locationCount: Int

    fun isAssignableTo(
        target: VerificationType,
        hierarchy: VerificationTypeHierarchy = VerificationTypeHierarchy.Empty,
    ): kotlin.Boolean =
        VerificationTypeLattice.isAssignable(source = this, target = target, hierarchy = hierarchy)

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

    data object ReturnAddress : VerificationType {
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

    data class ClassType(
        val internalName: String,
        val loader: String = "bootstrap",
    ) : VerificationType {
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

class VerificationTypeHierarchy(
    classes: Iterable<VerificationTypeClass> = emptyList(),
) {
    private val classesByKey: Map<VerificationTypeClassKey, VerificationTypeClass> =
        classes.associateBy { typeClass ->
            VerificationTypeClassKey(
                internalName = typeClass.internalName,
                loader = typeClass.loader,
            )
        }

    internal fun isWideningReference(source: VerificationType, target: VerificationType): kotlin.Boolean =
        source is VerificationType.ClassType &&
            target is VerificationType.ClassType &&
            isLoadedInterface(target)

    private fun isLoadedInterface(type: VerificationType.ClassType): kotlin.Boolean =
        classesByKey[VerificationTypeClassKey(type.internalName, type.loader)]?.isInterface == true

    companion object {
        val Empty: VerificationTypeHierarchy = VerificationTypeHierarchy()
    }
}

data class VerificationTypeClass(
    val internalName: String,
    val loader: String = "bootstrap",
    val isInterface: kotlin.Boolean = false,
)

data class VerificationTypeClassKey(
    val internalName: String,
    val loader: String = "bootstrap",
)

private object VerificationTypeLattice {
    fun isAssignable(
        source: VerificationType,
        target: VerificationType,
        hierarchy: VerificationTypeHierarchy,
    ): kotlin.Boolean {
        if (source == target) {
            return true
        }
        if (source == VerificationType.Null && target.isReferenceType()) {
            return true
        }
        if (hierarchy.isWideningReference(source = source, target = target)) {
            return true
        }
        return directSupertypes(source).any { supertype ->
            isAssignable(source = supertype, target = target, hierarchy = hierarchy)
        }
    }

    private fun VerificationType.isReferenceType(): kotlin.Boolean =
        this is VerificationType.ObjectType ||
            this is VerificationType.ClassType ||
            this is VerificationType.ArrayOf

    private fun directSupertypes(type: VerificationType): List<VerificationType> =
        when (type) {
            VerificationType.Top -> emptyList()
            VerificationType.ReturnAddress -> emptyList()
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
            is VerificationType.ClassType,
            is VerificationType.ArrayOf,
            -> listOf(VerificationType.Reference)
            VerificationType.UninitializedReference -> listOf(VerificationType.Reference)
            VerificationType.UninitializedThis,
            is VerificationType.Uninitialized,
            -> listOf(VerificationType.UninitializedReference)
        }
}
