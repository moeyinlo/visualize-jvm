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
    fun `runtime nestmates reject a member in a different package than its loaded host`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "host/Owner",
                    nestMemberInternalNames = listOf("other/Owner\$Nested"),
                ),
                JvmClassDefinition(
                    internalName = "other/Owner\$Nested",
                    nestHostInternalName = "host/Owner",
                ),
            ),
        )

        assertEquals(false, hierarchy.areRuntimeNestmates("host/Owner", "other/Owner\$Nested"))
        assertEquals(false, hierarchy.areRuntimeNestmates("other/Owner\$Nested", "host/Owner"))
    }

    @Test
    fun `runtime nestmates reject members when their nominated host is not self hosted`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "pkg/ActualHost",
                    nestMemberInternalNames = listOf("pkg/Host"),
                ),
                JvmClassDefinition(
                    internalName = "pkg/Host",
                    nestHostInternalName = "pkg/ActualHost",
                    nestMemberInternalNames = listOf("pkg/Host\$A", "pkg/Host\$B"),
                ),
                JvmClassDefinition(
                    internalName = "pkg/Host\$A",
                    nestHostInternalName = "pkg/Host",
                ),
                JvmClassDefinition(
                    internalName = "pkg/Host\$B",
                    nestHostInternalName = "pkg/Host",
                ),
            ),
        )

        assertEquals(false, hierarchy.areRuntimeNestmates("pkg/Host\$A", "pkg/Host\$B"))
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
