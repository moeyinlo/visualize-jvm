package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmClassExecutionMode
import me.moeyinlo.visualize.jvm.runtime.JvmClassExecutionPolicy
import me.moeyinlo.visualize.jvm.runtime.JvmClassInitializationState
import me.moeyinlo.visualize.jvm.runtime.JvmClassInitializationStates
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmWhitelistHostDelegationTest {
    @Test
    fun `explicit class whitelist delegates non platform host classes`() {
        val internalName = HostWhitelistFixture::class.java.name.replace('.', '/')
        val policy = JvmClassExecutionPolicy(
            hostDelegatedClassNames = setOf(internalName),
        )
        val loader = JvmIsolatedHostClassLoader(parent = HostWhitelistFixture::class.java.classLoader)

        assertEquals(JvmClassExecutionMode.HostDelegated, policy.modeFor(internalName))
        assertEquals(JvmClassExecutionMode.Interpreted, policy.modeFor("example/GuestOnly"))

        val mirror = loader.loadClassMirror(internalName)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "twice",
            descriptor = "(I)I",
        )

        val result = JvmHostMethodInvoker.invokeStatic(
            method = method,
            arguments = listOf(JvmIntValue(21)),
            heap = JvmHeap(),
        )

        assertEquals(JvmIntValue(42), result)
    }

    @Test
    fun `package prefix whitelist delegates matching non platform host classes`() {
        val internalName = HostWhitelistFixture::class.java.name.replace('.', '/')
        val packagePrefix = HostWhitelistFixture::class.java.packageName.replace('.', '/') + "/"
        val policy = JvmClassExecutionPolicy(
            hostDelegatedPackagePrefixes = setOf(packagePrefix),
        )
        val loader = JvmIsolatedHostClassLoader(parent = HostWhitelistFixture::class.java.classLoader)

        assertEquals(JvmClassExecutionMode.HostDelegated, policy.modeFor(internalName))

        val heap = JvmHeap()
        val mirror = loader.loadClassMirror(internalName)
        val hostReceiver = HostWhitelistFixture("host-")
        val guestReceiver = heap.allocateObject(internalName)
        val identityMap = JvmHostIdentityMap()
        identityMap.bind(guestReceiver, hostReceiver)
        val method = JvmHostMethodResolver.resolveInstanceMethod(
            owner = mirror,
            name = "label",
            descriptor = "(Ljava/lang/String;)Ljava/lang/String;",
        )

        val result = JvmHostMethodInvoker.invokeInstance(
            method = method,
            receiver = guestReceiver,
            arguments = listOf(heap.allocateString("delegated")),
            heap = heap,
            identityMap = identityMap,
        )

        val reference = result as JvmObjectReferenceValue
        assertEquals(JvmStringPayload("host-delegated"), heap.get(reference).payload)
    }

    @Test
    fun `explicit interpreted class overrides whitelist delegation`() {
        val internalName = HostWhitelistFixture::class.java.name.replace('.', '/')
        val packagePrefix = HostWhitelistFixture::class.java.packageName.replace('.', '/') + "/"
        val policy = JvmClassExecutionPolicy(
            hostDelegatedClassNames = setOf(internalName),
            hostDelegatedPackagePrefixes = setOf(packagePrefix),
            interpretedClassNames = setOf(internalName),
        )

        assertEquals(JvmClassExecutionMode.Interpreted, policy.modeFor(internalName))
    }

    @Test
    fun `writes delegated static fields through opaque initialization boundary`() {
        val internalName = HostWhitelistFixture::class.java.name.replace('.', '/')
        val policy = JvmClassExecutionPolicy(hostDelegatedClassNames = setOf(internalName))
        val recorder = JvmHostBoundaryEventRecorder()
        val initializationStates = JvmClassInitializationStates()
        val mirror = JvmIsolatedHostClassLoader(parent = HostWhitelistFixture::class.java.classLoader)
            .loadClassMirror(internalName)
        val field = JvmHostFieldResolver.resolveStaticField(
            owner = mirror,
            name = "mutableValue",
            descriptor = "I",
        )
        HostWhitelistFixture.mutableValue = 0

        JvmHostFieldAccessor.setStatic(
            field = field,
            value = JvmIntValue(17),
            heap = JvmHeap(),
            executionPolicy = policy,
            classInitializationStates = initializationStates,
            boundaryEvents = recorder,
        )

        assertEquals(17, HostWhitelistFixture.mutableValue)
        assertEquals(JvmClassInitializationState.Prepared, initializationStates.get(internalName))
        assertEquals(
            listOf(
                JvmHostBoundaryEventSnapshot(
                    sequence = 1,
                    action = JvmHostBoundaryAction.Delegated,
                    className = internalName,
                    methodName = "<clinit>",
                    descriptor = "()V",
                    detail = "host-delegated initialization is opaque to guest state",
                ),
            ),
            recorder.snapshots(),
        )
    }

    class HostWhitelistFixture(
        private val prefix: String = "",
    ) {
        fun label(value: String?): String = prefix + value

        companion object {
            @JvmField
            var mutableValue: Int = 0

            @JvmStatic
            fun twice(value: Int): Int = value * 2
        }
    }
}
