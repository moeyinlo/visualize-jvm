package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class ProtectedMemberAccessVerifierTest {
    private val thisType = VerificationType.ObjectType(ConstantPoolIndex(1))
    private val otherType = VerificationType.ObjectType(ConstantPoolIndex(2))

    @Test
    fun `array member owners bypass the protected member check`() {
        ProtectedMemberAccessVerifier.verify(
            access = access(owner = ProtectedMemberOwner.ArrayType("[Ljava/lang/String;")),
            environment = environment(),
            frame = frameWith(otherType),
        )
    }

    @Test
    fun `member owners not named as a superclass bypass the protected member check`() {
        ProtectedMemberAccessVerifier.verify(
            access = access(owner = ProtectedMemberOwner.ClassType("other/Helper")),
            environment = environment(
                superclasses = listOf(classDef("lib/Base", protectedMember())),
                loadedClasses = listOf(classDef("other/Helper", protectedMember())),
            ),
            frame = frameWith(otherType),
        )
    }

    @Test
    fun `protected superclass member in the same runtime package bypasses receiver narrowing`() {
        ProtectedMemberAccessVerifier.verify(
            access = access(owner = ProtectedMemberOwner.ClassType("pkg/Base")),
            environment = environment(
                currentClass = classDef("pkg/Sub"),
                superclasses = listOf(classDef("pkg/Base", protectedMember())),
                loadedClasses = listOf(classDef("pkg/Base", protectedMember())),
            ),
            frame = frameWith(otherType),
        )
    }

    @Test
    fun `non protected referenced member bypasses receiver narrowing after ambiguous superclass match`() {
        ProtectedMemberAccessVerifier.verify(
            access = access(owner = ProtectedMemberOwner.ClassType("lib/Base", loader = "app")),
            environment = environment(
                superclasses = listOf(classDef("lib/Base", protectedMember(), loader = "parent")),
                loadedClasses = listOf(classDef("lib/Base", publicMember(), loader = "app")),
            ),
            frame = frameWith(otherType),
        )
    }

    @Test
    fun `protected superclass member in another runtime package accepts receiver assignable to current class`() {
        ProtectedMemberAccessVerifier.verify(
            access = access(owner = ProtectedMemberOwner.ClassType("lib/Base")),
            environment = environment(
                superclasses = listOf(classDef("lib/Base", protectedMember())),
                loadedClasses = listOf(classDef("lib/Base", protectedMember())),
            ),
            frame = frameWith(thisType),
        )
    }

    @Test
    fun `protected superclass member in another runtime package accepts null receiver`() {
        ProtectedMemberAccessVerifier.verify(
            access = access(owner = ProtectedMemberOwner.ClassType("lib/Base")),
            environment = environment(
                superclasses = listOf(classDef("lib/Base", protectedMember())),
                loadedClasses = listOf(classDef("lib/Base", protectedMember())),
            ),
            frame = frameWith(VerificationType.Null),
        )
    }

    @Test
    fun `protected superclass member in another runtime package rejects unrelated receiver`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ProtectedMemberAccessVerifier.verify(
                access = access(owner = ProtectedMemberOwner.ClassType("lib/Base")),
                environment = environment(
                    superclasses = listOf(classDef("lib/Base", protectedMember())),
                    loadedClasses = listOf(classDef("lib/Base", protectedMember())),
                ),
                frame = frameWith(otherType),
            )
        }

        assertEquals(
            "Protected member lib/Base.p:I requires receiver assignable to current class pkg/Sub at bytecode offset 42, but found ObjectType(constantPoolIndex=#2)",
            exception.message,
        )
    }

    @Test
    fun `protected superclass member in another runtime package requires an object receiver`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ProtectedMemberAccessVerifier.verify(
                access = access(owner = ProtectedMemberOwner.ClassType("lib/Base")),
                environment = environment(
                    superclasses = listOf(classDef("lib/Base", protectedMember())),
                    loadedClasses = listOf(classDef("lib/Base", protectedMember())),
                ),
                frame = VerificationFrameState(bytecodeOffset = 42, locals = emptyList(), stack = emptyList()),
            )
        }

        assertEquals(
            "Protected member lib/Base.p:I requires an object receiver at bytecode offset 42",
            exception.message,
        )
    }

    private fun environment(
        currentClass: ProtectedVerifierClass = classDef("pkg/Sub"),
        superclasses: List<ProtectedVerifierClass> = emptyList(),
        loadedClasses: List<ProtectedVerifierClass> = superclasses,
    ): ProtectedMemberAccessEnvironment =
        ProtectedMemberAccessEnvironment(
            currentClass = currentClass,
            thisType = thisType,
            superclasses = superclasses,
            loadedClasses = loadedClasses.associateBy { ProtectedClassKey(it.internalName, it.definingLoader) },
        )

    private fun access(owner: ProtectedMemberOwner): ProtectedMemberAccess =
        ProtectedMemberAccess(
            owner = owner,
            name = "p",
            descriptor = "I",
        )

    private fun classDef(
        internalName: String,
        vararg members: ProtectedClassMember,
        loader: String = "bootstrap",
    ): ProtectedVerifierClass =
        ProtectedVerifierClass(
            internalName = internalName,
            definingLoader = loader,
            members = members.toList(),
        )

    private fun protectedMember(): ProtectedClassMember =
        ProtectedClassMember(name = "p", descriptor = "I", isProtected = true)

    private fun publicMember(): ProtectedClassMember =
        ProtectedClassMember(name = "p", descriptor = "I", isProtected = false)

    private fun frameWith(receiver: VerificationType): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 42, locals = emptyList(), stack = listOf(receiver))
}
