package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmMethodCompletionTest {
    @Test
    fun `void methods complete normally without a return value`() {
        val method = resolvedMethod(descriptor = "()V")

        assertEquals(
            JvmMethodCompletion.Normal(method = method, returnValue = null),
            method.normalCompletion(),
        )
    }

    @Test
    fun `value returning methods complete normally with descriptor-compatible values`() {
        val intMethod = resolvedMethod(descriptor = "()I")
        val longMethod = resolvedMethod(descriptor = "()J")
        val floatMethod = resolvedMethod(descriptor = "()F")
        val doubleMethod = resolvedMethod(descriptor = "()D")
        val referenceMethod = resolvedMethod(descriptor = "()Ljava/lang/String;")
        val arrayMethod = resolvedMethod(descriptor = "()[I")
        val reference = JvmObjectReferenceValue(JvmReferenceId(1))

        assertEquals(JvmMethodCompletion.Normal(intMethod, JvmIntValue(1)), intMethod.normalCompletion(JvmIntValue(1)))
        assertEquals(JvmMethodCompletion.Normal(longMethod, JvmLongValue(2)), longMethod.normalCompletion(JvmLongValue(2)))
        assertEquals(JvmMethodCompletion.Normal(floatMethod, JvmFloatValue(3.0f)), floatMethod.normalCompletion(JvmFloatValue(3.0f)))
        assertEquals(JvmMethodCompletion.Normal(doubleMethod, JvmDoubleValue(4.0)), doubleMethod.normalCompletion(JvmDoubleValue(4.0)))
        assertEquals(JvmMethodCompletion.Normal(referenceMethod, reference), referenceMethod.normalCompletion(reference))
        assertEquals(JvmMethodCompletion.Normal(arrayMethod, JvmNullValue), arrayMethod.normalCompletion(JvmNullValue))
    }

    @Test
    fun `small integral return descriptors complete with int values`() {
        listOf("()Z", "()B", "()C", "()S").forEach { descriptor ->
            val method = resolvedMethod(descriptor = descriptor)

            assertEquals(
                JvmMethodCompletion.Normal(method, JvmIntValue(1)),
                method.normalCompletion(JvmIntValue(1)),
            )
        }
    }

    @Test
    fun `normal completion rejects missing unexpected or incompatible return values`() {
        assertFailsWith<JvmMethodCompletionException> {
            resolvedMethod(descriptor = "()V").normalCompletion(JvmIntValue(1))
        }
        assertFailsWith<JvmMethodCompletionException> {
            resolvedMethod(descriptor = "()I").normalCompletion()
        }
        assertFailsWith<JvmMethodCompletionException> {
            resolvedMethod(descriptor = "()J").normalCompletion(JvmIntValue(1))
        }
        assertFailsWith<JvmMethodCompletionException> {
            resolvedMethod(descriptor = "()Ljava/lang/Object;").normalCompletion(JvmIntValue(1))
        }
    }

    @Test
    fun `normal completion rejects invalid method descriptors`() {
        assertFailsWith<IllegalArgumentException> {
            resolvedMethod(descriptor = "I)V").normalCompletion(JvmIntValue(1))
        }
        assertFailsWith<IllegalArgumentException> {
            resolvedMethod(descriptor = "()" ).normalCompletion()
        }
    }

    @Test
    fun `methods complete abruptly with a guest throwable reference`() {
        val method = resolvedMethod(descriptor = "()V")
        val throwable = JvmObjectReferenceValue(JvmReferenceId(7))

        assertEquals(
            JvmMethodCompletion.Abrupt(method = method, throwable = throwable),
            method.abruptCompletion(throwable),
        )
    }

    private fun resolvedMethod(descriptor: String): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "Example",
        name = "run",
        descriptor = descriptor,
        isStatic = true,
    )
}
