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

    @Test
    fun `class verification type widens between different initiating loaders for the same loaded class`() {
        val sharedDefinition = VerificationTypeClassKey("pkg/Shared", loader = "defining")
        val hierarchy = VerificationTypeHierarchy(
            classes = listOf(
                VerificationTypeClass(
                    internalName = "pkg/Shared",
                    loader = "loaderA",
                    definition = sharedDefinition,
                ),
                VerificationTypeClass(
                    internalName = "pkg/Shared",
                    loader = "loaderB",
                    definition = sharedDefinition,
                ),
                VerificationTypeClass(
                    internalName = "pkg/Shared",
                    loader = "loaderC",
                    definition = VerificationTypeClassKey("pkg/Shared", loader = "otherDefining"),
                ),
            ),
        )

        assertTrue(
            VerificationType.ClassType("pkg/Shared", loader = "loaderA").isAssignableTo(
                VerificationType.ClassType("pkg/Shared", loader = "loaderB"),
                hierarchy = hierarchy,
            ),
        )
        assertFalse(
            VerificationType.ClassType("pkg/Shared", loader = "loaderA").isAssignableTo(
                VerificationType.ClassType("pkg/Shared", loader = "loaderC"),
                hierarchy = hierarchy,
            ),
        )
        assertFalse(
            VerificationType.ClassType("pkg/Shared", loader = "loaderA").isAssignableTo(
                VerificationType.ClassType("pkg/Shared", loader = "missingLoader"),
                hierarchy = hierarchy,
            ),
        )
    }

    @Test
    fun `array verification type widens to bootstrap defined Object`() {
        val hierarchy = VerificationTypeHierarchy(
            classes = listOf(
                VerificationTypeClass(
                    internalName = "java/lang/Object",
                    loader = "app",
                    definition = VerificationTypeClassKey("java/lang/Object", loader = "bootstrap"),
                ),
                VerificationTypeClass(
                    internalName = "java/lang/Object",
                    loader = "custom",
                    definition = VerificationTypeClassKey("java/lang/Object", loader = "custom"),
                ),
            ),
        )

        assertTrue(
            VerificationType.ArrayOf(VerificationType.Integer).isAssignableTo(
                VerificationType.ClassType("java/lang/Object", loader = "app"),
                hierarchy = hierarchy,
            ),
        )
        assertFalse(
            VerificationType.ArrayOf(VerificationType.Integer).isAssignableTo(
                VerificationType.ClassType("java/lang/Object", loader = "missing"),
                hierarchy = hierarchy,
            ),
        )
        assertFalse(
            VerificationType.ArrayOf(VerificationType.Integer).isAssignableTo(
                VerificationType.ClassType("java/lang/Object", loader = "custom"),
                hierarchy = hierarchy,
            ),
        )
    }
}
