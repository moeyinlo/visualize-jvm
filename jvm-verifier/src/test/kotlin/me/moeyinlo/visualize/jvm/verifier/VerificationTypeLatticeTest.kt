package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerificationTypeLatticeTest {
    @Test
    fun `class verification type is a one word reference type`() {
        val stringType = VerificationType.ClassType("java/lang/String")

        assertTrue(stringType.isAssignableTo(VerificationType.Reference))
        assertTrue(stringType.isAssignableTo(VerificationType.OneWord))
        assertTrue(stringType.isAssignableTo(VerificationType.Top))
        assertFalse(stringType.isAssignableTo(VerificationType.TwoWord))
    }

    @Test
    fun `null is assignable to class verification type`() {
        assertTrue(VerificationType.Null.isAssignableTo(VerificationType.ClassType("java/lang/Object")))
    }

    @Test
    fun `class verification type widens to any loaded interface type`() {
        val hierarchy = VerificationTypeHierarchy(
            classes = listOf(
                VerificationTypeClass(
                    internalName = "pkg/Api",
                    isInterface = true,
                ),
                VerificationTypeClass(
                    internalName = "pkg/Concrete",
                    isInterface = false,
                ),
            ),
        )

        assertTrue(
            VerificationType.ClassType("pkg/Impl").isAssignableTo(
                VerificationType.ClassType("pkg/Api"),
                hierarchy = hierarchy,
            ),
        )
        assertFalse(
            VerificationType.ClassType("pkg/Impl").isAssignableTo(
                VerificationType.ClassType("pkg/MissingInterface"),
                hierarchy = hierarchy,
            ),
        )
        assertFalse(
            VerificationType.ClassType("pkg/Impl").isAssignableTo(
                VerificationType.ClassType("pkg/Concrete"),
                hierarchy = hierarchy,
            ),
        )
    }

    @Test
    fun `class verification type widens to a loaded superclass type`() {
        val hierarchy = VerificationTypeHierarchy(
            classes = listOf(
                VerificationTypeClass(
                    internalName = "pkg/Base",
                ),
                VerificationTypeClass(
                    internalName = "pkg/Sibling",
                ),
                VerificationTypeClass(
                    internalName = "pkg/Sub",
                    superclasses = listOf(VerificationTypeClassKey("pkg/Base")),
                ),
            ),
        )

        assertTrue(
            VerificationType.ClassType("pkg/Sub").isAssignableTo(
                VerificationType.ClassType("pkg/Base"),
                hierarchy = hierarchy,
            ),
        )
        assertFalse(
            VerificationType.ClassType("pkg/Sub").isAssignableTo(
                VerificationType.ClassType("pkg/Sibling"),
                hierarchy = hierarchy,
            ),
        )
        assertFalse(
            VerificationType.ClassType("pkg/MissingSub").isAssignableTo(
                VerificationType.ClassType("pkg/Base"),
                hierarchy = hierarchy,
            ),
        )
        assertFalse(
            VerificationType.ClassType("pkg/Sub").isAssignableTo(
                VerificationType.ClassType("pkg/MissingBase"),
                hierarchy = hierarchy,
            ),
        )
    }
}
