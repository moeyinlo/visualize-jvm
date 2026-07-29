package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmVerificationStrategySelectorTest {
    @Test
    fun `legacy classfile versions select type inference verification`() {
        assertEquals(
            JvmVerificationStrategy.TypeInference,
            JvmVerificationStrategySelector.select(JvmVerificationInput(majorVersion = 49, hasStackMapTable = false)),
        )
    }

    @Test
    fun `Java 6 and newer classfile versions select type checking when stack maps are present`() {
        assertEquals(
            JvmVerificationStrategy.TypeChecking,
            JvmVerificationStrategySelector.select(JvmVerificationInput(majorVersion = 50, hasStackMapTable = true)),
        )
        assertEquals(
            JvmVerificationStrategy.TypeChecking,
            JvmVerificationStrategySelector.select(JvmVerificationInput(majorVersion = 70, hasStackMapTable = true)),
        )
    }

    @Test
    fun `Java 6 and newer classfile versions without stack maps are identified as needing inferred frames`() {
        assertEquals(
            JvmVerificationStrategy.TypeCheckingWithInferredFrames,
            JvmVerificationStrategySelector.select(JvmVerificationInput(majorVersion = 50, hasStackMapTable = false)),
        )
    }

    @Test
    fun `verification inputs validate classfile versions`() {
        assertFailsWith<IllegalArgumentException> {
            JvmVerificationInput(majorVersion = 0, hasStackMapTable = false)
        }
    }
}
