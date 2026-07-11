package me.moeyinlo.visualize.jvm.verifier

object ProtectedMemberAccessVerifier {
    fun verify(
        access: ProtectedMemberAccess,
        environment: ProtectedMemberAccessEnvironment,
        frame: VerificationFrameState,
    ) {
        val owner = access.owner
        if (owner !is ProtectedMemberOwner.ClassType) {
            return
        }

        if (!environment.hasSuperclassNamed(owner.internalName)) {
            return
        }

        val matchingOtherPackageProtectedSupers = environment.superclasses.filter { superclass ->
            superclass.internalName == owner.internalName &&
                !environment.sameRuntimePackage(environment.currentClass, superclass) &&
                superclass.hasProtectedMember(access.name, access.descriptor)
        }
        if (matchingOtherPackageProtectedSupers.isEmpty()) {
            return
        }

        val referencedClass = environment.loadedClasses[ProtectedClassKey(owner.internalName, owner.loader)]
            ?: throw MethodVerificationException(
                "Protected member ${access.displayName()} requires loaded referenced class " +
                    "${owner.internalName} from loader ${owner.loader}",
            )
        if (!referencedClass.hasProtectedMember(access.name, access.descriptor)) {
            return
        }

        val receiver = frame.stack.lastOrNull()
            ?: throw MethodVerificationException(
                "Protected member ${access.displayName()} requires an object receiver at bytecode offset " +
                    frame.bytecodeOffset,
            )
        if (!environment.isAssignableToThis(receiver, environment.thisType)) {
            throw MethodVerificationException(
                "Protected member ${access.displayName()} requires receiver assignable to current class " +
                    "${environment.currentClass.internalName} at bytecode offset ${frame.bytecodeOffset}, " +
                    "but found $receiver",
            )
        }
    }

    private fun ProtectedMemberAccessEnvironment.hasSuperclassNamed(internalName: String): Boolean =
        superclasses.any { superclass -> superclass.internalName == internalName }

    private fun ProtectedVerifierClass.hasProtectedMember(name: String, descriptor: String): Boolean =
        members.any { member ->
            member.name == name && member.descriptor == descriptor && member.isProtected
        }

    private fun ProtectedMemberAccess.displayName(): String =
        when (owner) {
            is ProtectedMemberOwner.ArrayType -> "${owner.descriptor}.$name:$descriptor"
            is ProtectedMemberOwner.ClassType -> "${owner.internalName}.$name:$descriptor"
        }
}

sealed interface ProtectedMemberOwner {
    data class ClassType(
        val internalName: String,
        val loader: String = "bootstrap",
    ) : ProtectedMemberOwner

    data class ArrayType(
        val descriptor: String,
    ) : ProtectedMemberOwner
}

data class ProtectedMemberAccess(
    val owner: ProtectedMemberOwner,
    val name: String,
    val descriptor: String,
)

data class ProtectedMemberAccessEnvironment(
    val currentClass: ProtectedVerifierClass,
    val thisType: VerificationType,
    val superclasses: List<ProtectedVerifierClass>,
    val loadedClasses: Map<ProtectedClassKey, ProtectedVerifierClass>,
    val isAssignableToThis: (target: VerificationType, thisType: VerificationType) -> Boolean = { target, thisType ->
        target.isAssignableTo(thisType)
    },
) {
    fun sameRuntimePackage(first: ProtectedVerifierClass, second: ProtectedVerifierClass): Boolean =
        first.definingLoader == second.definingLoader && first.packageName == second.packageName
}

data class ProtectedClassKey(
    val internalName: String,
    val loader: String = "bootstrap",
)

data class ProtectedVerifierClass(
    val internalName: String,
    val definingLoader: String = "bootstrap",
    val members: List<ProtectedClassMember> = emptyList(),
) {
    val packageName: String = internalName.substringBeforeLast('/', missingDelimiterValue = "")
}

data class ProtectedClassMember(
    val name: String,
    val descriptor: String,
    val isProtected: Boolean,
)
