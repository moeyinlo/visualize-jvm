package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmClassHierarchyNestmateTest {
    @Test
    fun `runtime nestmates accept a host and a named nest member`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Owner",
                    nestMemberInternalNames = listOf("Owner\$Nested"),
                ),
                JvmClassDefinition(
                    internalName = "Owner\$Nested",
                    nestHostInternalName = "Owner",
                ),
            ),
        )

        assertEquals(true, hierarchy.areRuntimeNestmates("Owner", "Owner\$Nested"))
        assertEquals(true, hierarchy.areRuntimeNestmates("Owner\$Nested", "Owner"))
    }

    @Test
    fun `runtime nestmates reject a member not named by its loaded host`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "Owner"),
                JvmClassDefinition(
                    internalName = "Owner\$Nested",
                    nestHostInternalName = "Owner",
                ),
            ),
        )

        assertEquals(false, hierarchy.areRuntimeNestmates("Owner", "Owner\$Nested"))
    }

    @Test
    fun `runtime nestmates reject members whose host class is not loaded`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Owner\$Nested",
                    nestHostInternalName = "Owner",
                ),
            ),
        )

        assertEquals(false, hierarchy.areRuntimeNestmates("Owner\$Nested", "Owner\$Nested"))
    }
}
