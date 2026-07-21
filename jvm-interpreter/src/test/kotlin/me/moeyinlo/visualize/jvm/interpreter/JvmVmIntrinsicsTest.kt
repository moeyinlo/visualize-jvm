package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorState
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import me.moeyinlo.visualize.jvm.runtime.JvmStackTraceFrame
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import me.moeyinlo.visualize.jvm.runtime.JvmThreadPayload
import me.moeyinlo.visualize.jvm.runtime.JvmThrowablePayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class JvmVmIntrinsicsTest {
    @Test
    fun `Object getClass intrinsic returns an interned guest class mirror for the receiver class`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectGetClassMethod())
            ?: error("Object.getClass intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        val classMirror = result as JvmObjectReferenceValue
        assertEquals(classMirror, secondResult)
        val classMirrorObject = heap.get(classMirror)
        assertEquals("java/lang/Class", classMirrorObject.className)
        assertEquals(JvmClassPayload("Example"), classMirrorObject.payload)
    }

    @Test
    fun `Object getClass intrinsic rejects missing receivers`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectGetClassMethod())
            ?: error("Object.getClass intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = null,
        )

        assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(
                context,
                JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
            )
        }
    }

    @Test
    fun `Object hashCode intrinsic returns a stable identity hash for guest objects`() {
        val heap = JvmHeap()
        val firstReceiver = heap.allocateObject("Example")
        val secondReceiver = heap.allocateObject("Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectHashCodeMethod())
            ?: error("Object.hashCode intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
        )

        val firstHash = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = firstReceiver, arguments = emptyList()),
        )
        val repeatedFirstHash = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = firstReceiver, arguments = emptyList()),
        )
        val secondHash = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = secondReceiver, arguments = emptyList()),
        )

        assertEquals(firstHash, repeatedFirstHash)
        assertNotEquals(firstHash, secondHash)
        assertEquals(JvmIntValue::class, firstHash!!::class)
    }

    @Test
    fun `Object hashCode intrinsic rejects missing receivers`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectHashCodeMethod())
            ?: error("Object.hashCode intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = null,
        )

        assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(
                context,
                JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
            )
        }
    }

    @Test
    fun `Object clone intrinsic shallow clones Cloneable guest objects`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val field = JvmFieldReference(ownerClassName = "Example", name = "counter", descriptor = "I")
        heap.putInstanceField(receiver, field, JvmIntValue(42))
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectCloneMethod())
            ?: error("Object.clone intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        interfaceNames = listOf("java/lang/Cloneable"),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        assertNotEquals(receiver, result)
        assertEquals("Example", heap.get(result).className)
        assertEquals(JvmIntValue(42), heap.getInstanceField(result, field))
    }

    @Test
    fun `Object clone intrinsic shallow clones guest arrays`() {
        val heap = JvmHeap()
        val receiver = heap.allocateIntArray(2)
        val originalPayload = heap.get(receiver).payload as JvmIntArrayPayload
        originalPayload.elements[0] = 3
        originalPayload.elements[1] = 4
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectCloneMethod())
            ?: error("Object.clone intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = null,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        assertNotEquals(receiver, result)
        val clonedPayload = heap.get(result).payload as JvmIntArrayPayload
        assertEquals(mutableListOf(3, 4), clonedPayload.elements)
        originalPayload.elements[1] = 9
        assertEquals(mutableListOf(3, 4), clonedPayload.elements)
    }

    @Test
    fun `Object clone intrinsic rejects non Cloneable guest objects`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectCloneMethod())
            ?: error("Object.clone intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(listOf(JvmClassDefinition(internalName = "Example"))),
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
        )

        assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(
                context,
                JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
            )
        }
    }

    @Test
    fun `Object wait intrinsic releases the receiver monitor and records the current thread as waiting`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val receiver = heap.allocateObject("Example")
        monitors.enter(receiver, threadId = "main")
        monitors.enter(receiver, threadId = "main")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectWaitLongMethod())
            ?: error("Object.wait intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            currentThreadId = "main",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = listOf(JvmLongValue(0L))),
        )

        assertEquals(null, result)
        assertEquals(0, monitors.holdCount(receiver, threadId = "main"))
        assertEquals(listOf("main"), monitors.waitingThreads(receiver))
    }

    @Test
    fun `Object notify intrinsic wakes one waiting thread from the receiver monitor`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val receiver = heap.allocateObject("Example")
        monitors.enter(receiver, threadId = "first")
        monitors.waitForNotification(receiver, threadId = "first")
        monitors.enter(receiver, threadId = "second")
        monitors.waitForNotification(receiver, threadId = "second")
        monitors.enter(receiver, threadId = "owner")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectNotifyMethod())
            ?: error("Object.notify intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            currentThreadId = "owner",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        assertEquals(null, result)
        assertEquals(listOf("second"), monitors.waitingThreads(receiver))
    }

    @Test
    fun `Object notifyAll intrinsic wakes every waiting thread from the receiver monitor`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val receiver = heap.allocateObject("Example")
        monitors.enter(receiver, threadId = "first")
        monitors.waitForNotification(receiver, threadId = "first")
        monitors.enter(receiver, threadId = "second")
        monitors.waitForNotification(receiver, threadId = "second")
        monitors.enter(receiver, threadId = "owner")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectNotifyAllMethod())
            ?: error("Object.notifyAll intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            currentThreadId = "owner",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        assertEquals(null, result)
        assertEquals(emptyList(), monitors.waitingThreads(receiver))
    }

    @Test
    fun `System arraycopy intrinsic copies primitive arrays with overlap semantics`() {
        val heap = JvmHeap()
        val array = heap.allocateIntArray(5)
        val payload = heap.get(array).payload as JvmIntArrayPayload
        payload.elements[0] = 1
        payload.elements[1] = 2
        payload.elements[2] = 3
        payload.elements[3] = 4
        payload.elements[4] = 5
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemArraycopyMethod())
            ?: error("System.arraycopy intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                receiver = null,
                arguments = listOf(array, JvmIntValue(0), array, JvmIntValue(1), JvmIntValue(3)),
            ),
        )

        assertEquals(null, result)
        assertEquals(mutableListOf(1, 1, 2, 3, 5), payload.elements)
    }

    @Test
    fun `System arraycopy intrinsic copies assignable reference array elements`() {
        val heap = JvmHeap()
        val child = heap.allocateObject("Child")
        val source = heap.allocateReferenceArray("Child", 2)
        val target = heap.allocateReferenceArray("Base", 2)
        val sourcePayload = heap.get(source).payload as JvmReferenceArrayPayload
        sourcePayload.elements[0] = child
        sourcePayload.elements[1] = JvmNullValue
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemArraycopyMethod())
            ?: error("System.arraycopy intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Base"),
                    JvmClassDefinition(internalName = "Child", superclassName = "Base"),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                receiver = null,
                arguments = listOf(source, JvmIntValue(0), target, JvmIntValue(0), JvmIntValue(2)),
            ),
        )

        assertEquals(
            JvmReferenceArrayPayload(mutableListOf(child, JvmNullValue)),
            heap.get(target).payload,
        )
    }

    @Test
    fun `System arraycopy intrinsic rejects mismatched primitive array types`() {
        val heap = JvmHeap()
        val source = heap.allocateIntArray(1)
        val target = heap.allocateLongArray(1)
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemArraycopyMethod())
            ?: error("System.arraycopy intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(
                context,
                JvmNativeMethodInvocation(
                    receiver = null,
                    arguments = listOf(source, JvmIntValue(0), target, JvmIntValue(0), JvmIntValue(1)),
                ),
            )
        }
    }

    @Test
    fun `System identityHashCode intrinsic returns a stable identity hash for guest objects`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemIdentityHashCodeMethod())
            ?: error("System.identityHashCode intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        val firstHash = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = listOf(receiver)),
        )
        val secondHash = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = listOf(receiver)),
        )

        assertEquals(firstHash, secondHash)
        assertEquals(JvmIntValue::class, firstHash!!::class)
    }

    @Test
    fun `System identityHashCode intrinsic returns zero for null`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemIdentityHashCodeMethod())
            ?: error("System.identityHashCode intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = listOf(JvmNullValue)),
        )

        assertEquals(JvmIntValue(0), result)
    }

    @Test
    fun `System currentTimeMillis intrinsic returns the context wall clock value`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemCurrentTimeMillisMethod())
            ?: error("System.currentTimeMillis intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
            currentTimeMillisProvider = { 123_456_789L },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(JvmLongValue(123_456_789L), result)
    }

    @Test
    fun `System nanoTime intrinsic returns the context monotonic clock value`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemNanoTimeMethod())
            ?: error("System.nanoTime intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
            nanoTimeProvider = { 987_654_321L },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(JvmLongValue(987_654_321L), result)
    }

    @Test
    fun `Class initClassName intrinsic returns the guest binary name string`() {
        val heap = JvmHeap()
        val classMirror = heap.internClassMirror("pkg/Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(classInitClassNameMethod())
            ?: error("Class.initClassName intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = classMirror, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        assertEquals(JvmStringPayload("pkg.Example"), heap.get(result).payload)
    }

    @Test
    fun `Class query intrinsics report array primitive and interface mirrors`() {
        val heap = JvmHeap()
        val arrayMirror = heap.internClassMirror("[Ljava/lang/String;")
        val primitiveMirror = heap.internClassMirror("int")
        val interfaceMirror = heap.internClassMirror("pkg/Service")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(JvmClassDefinition(internalName = "pkg/Service", isInterface = true)),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        val isArray = JvmVmIntrinsics.Registry.resolve(classIsArrayMethod())
            ?: error("Class.isArray intrinsic was not registered")
        val isPrimitive = JvmVmIntrinsics.Registry.resolve(classIsPrimitiveMethod())
            ?: error("Class.isPrimitive intrinsic was not registered")
        val isInterface = JvmVmIntrinsics.Registry.resolve(classIsInterfaceMethod())
            ?: error("Class.isInterface intrinsic was not registered")

        assertEquals(JvmIntValue(1), isArray.invoke(context, JvmNativeMethodInvocation(arrayMirror, emptyList())))
        assertEquals(JvmIntValue(0), isArray.invoke(context, JvmNativeMethodInvocation(primitiveMirror, emptyList())))
        assertEquals(JvmIntValue(1), isPrimitive.invoke(context, JvmNativeMethodInvocation(primitiveMirror, emptyList())))
        assertEquals(JvmIntValue(0), isPrimitive.invoke(context, JvmNativeMethodInvocation(arrayMirror, emptyList())))
        assertEquals(JvmIntValue(1), isInterface.invoke(context, JvmNativeMethodInvocation(interfaceMirror, emptyList())))
    }

    @Test
    fun `Class getSuperclass intrinsic returns object for arrays and declared superclass for ordinary classes`() {
        val heap = JvmHeap()
        val arrayMirror = heap.internClassMirror("[I")
        val childMirror = heap.internClassMirror("pkg/Child")
        val objectMirror = heap.internClassMirror("java/lang/Object")
        val primitiveMirror = heap.internClassMirror("int")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(classGetSuperclassMethod())
            ?: error("Class.getSuperclass intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(JvmClassDefinition(internalName = "pkg/Child", superclassName = "pkg/Base")),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        val arraySuperclass = intrinsic.invoke(context, JvmNativeMethodInvocation(arrayMirror, emptyList()))
        val childSuperclass = intrinsic.invoke(context, JvmNativeMethodInvocation(childMirror, emptyList()))
        val objectSuperclass = intrinsic.invoke(context, JvmNativeMethodInvocation(objectMirror, emptyList()))
        val primitiveSuperclass = intrinsic.invoke(context, JvmNativeMethodInvocation(primitiveMirror, emptyList()))

        assertEquals(heap.internClassMirror("java/lang/Object"), arraySuperclass)
        assertEquals(heap.internClassMirror("pkg/Base"), childSuperclass)
        assertEquals(JvmNullValue, objectSuperclass)
        assertEquals(JvmNullValue, primitiveSuperclass)
    }

    @Test
    fun `Throwable fillInStackTrace intrinsic records context stack trace and returns receiver`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("java/lang/Throwable")
        val frame = JvmStackTraceFrame(
            declaringClass = "pkg/Example",
            methodName = "call",
            fileName = "Example.java",
            lineNumber = 42,
        )
        val intrinsic = JvmVmIntrinsics.Registry.resolve(throwableFillInStackTraceMethod())
            ?: error("Throwable.fillInStackTrace intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Throwable", superclassName = "java/lang/Object"),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Throwable",
            stackTraceProvider = { listOf(frame) },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = listOf(JvmIntValue(0))),
        )

        assertEquals(receiver, result)
        assertEquals(JvmThrowablePayload(listOf(frame)), heap.get(receiver).payload)
    }

    @Test
    fun `String intern intrinsic returns the canonical guest string reference`() {
        val heap = JvmHeap()
        val receiver = heap.allocateString("hello")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(stringInternMethod())
            ?: error("String.intern intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/String",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        assertEquals(heap.internString("hello"), result)
        assertEquals(JvmStringPayload("hello"), heap.get(result).payload)
    }

    @Test
    fun `Thread currentThread intrinsic returns the current guest thread mirror`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadCurrentThreadMethod())
            ?: error("Thread.currentThread intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
            currentThreadId = "worker-1",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        ) as JvmObjectReferenceValue
        val repeated = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(result, repeated)
        assertEquals("java/lang/Thread", heap.get(result).className)
        assertEquals(JvmThreadPayload("worker-1"), heap.get(result).payload)
    }

    @Test
    fun `Thread sleep intrinsics delegate validated guest sleep requests to the context`() {
        val heap = JvmHeap()
        val sleeps = mutableListOf<Pair<Long, Int>>()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
            threadSleepHandler = { millis, nanos -> sleeps += millis to nanos },
        )
        val sleepMillis = JvmVmIntrinsics.Registry.resolve(threadSleepMillisMethod())
            ?: error("Thread.sleep(J) intrinsic was not registered")
        val sleepMillisNanos = JvmVmIntrinsics.Registry.resolve(threadSleepMillisNanosMethod())
            ?: error("Thread.sleep(JI) intrinsic was not registered")
        val sleepNanos0 = JvmVmIntrinsics.Registry.resolve(threadSleepNanos0Method())
            ?: error("Thread.sleepNanos0(J) intrinsic was not registered")

        assertEquals(
            null,
            sleepMillis.invoke(context, JvmNativeMethodInvocation(null, listOf(JvmLongValue(5L)))),
        )
        assertEquals(
            null,
            sleepMillisNanos.invoke(context, JvmNativeMethodInvocation(null, listOf(JvmLongValue(6L), JvmIntValue(7)))),
        )
        assertEquals(
            null,
            sleepNanos0.invoke(context, JvmNativeMethodInvocation(null, listOf(JvmLongValue(1_234_567L)))),
        )

        assertEquals(listOf(5L to 0, 6L to 7, 1L to 234_567), sleeps)
    }

    @Test
    fun `Thread sleep intrinsics reject negative and out of range guest sleep requests`() {
        val heap = JvmHeap()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )
        val sleepMillis = JvmVmIntrinsics.Registry.resolve(threadSleepMillisMethod())
            ?: error("Thread.sleep(J) intrinsic was not registered")
        val sleepMillisNanos = JvmVmIntrinsics.Registry.resolve(threadSleepMillisNanosMethod())
            ?: error("Thread.sleep(JI) intrinsic was not registered")

        assertFailsWith<JvmUnsupportedInstructionException> {
            sleepMillis.invoke(context, JvmNativeMethodInvocation(null, listOf(JvmLongValue(-1L))))
        }
        assertFailsWith<JvmUnsupportedInstructionException> {
            sleepMillisNanos.invoke(
                context,
                JvmNativeMethodInvocation(null, listOf(JvmLongValue(0L), JvmIntValue(1_000_000))),
            )
        }
    }

    @Test
    fun `VM intrinsic registry resolves the Phase 15 native intrinsic surface`() {
        val phase15Methods = listOf(
            objectGetClassMethod(),
            objectHashCodeMethod(),
            objectCloneMethod(),
            objectWaitLongMethod(),
            objectNotifyMethod(),
            objectNotifyAllMethod(),
            systemArraycopyMethod(),
            systemIdentityHashCodeMethod(),
            systemCurrentTimeMillisMethod(),
            systemNanoTimeMethod(),
            classInitClassNameMethod(),
            classIsArrayMethod(),
            classIsPrimitiveMethod(),
            classIsInterfaceMethod(),
            classGetSuperclassMethod(),
            throwableFillInStackTraceMethod(),
            stringInternMethod(),
            threadCurrentThreadMethod(),
            threadSleepMillisMethod(),
            threadSleepMillisNanosMethod(),
            threadSleepNanos0Method(),
        )

        phase15Methods.forEach { method ->
            assertNotNull(
                JvmVmIntrinsics.Registry.resolve(method),
                "Expected intrinsic for ${method.ownerClassName}.${method.name}:${method.descriptor}",
            )
        }
    }

    private fun objectGetClassMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "getClass",
        descriptor = "()Ljava/lang/Class;",
        isStatic = false,
        isNative = true,
    )

    private fun objectHashCodeMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "hashCode",
        descriptor = "()I",
        isStatic = false,
        isNative = true,
    )

    private fun objectCloneMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "clone",
        descriptor = "()Ljava/lang/Object;",
        isStatic = false,
        isNative = true,
    )

    private fun objectWaitLongMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "wait",
        descriptor = "(J)V",
        isStatic = false,
        isNative = true,
    )

    private fun objectNotifyMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "notify",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun objectNotifyAllMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "notifyAll",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun systemArraycopyMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "arraycopy",
        descriptor = "(Ljava/lang/Object;ILjava/lang/Object;II)V",
        isStatic = true,
        isNative = true,
    )

    private fun systemIdentityHashCodeMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "identityHashCode",
        descriptor = "(Ljava/lang/Object;)I",
        isStatic = true,
        isNative = true,
    )

    private fun systemCurrentTimeMillisMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "currentTimeMillis",
        descriptor = "()J",
        isStatic = true,
        isNative = true,
    )

    private fun systemNanoTimeMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "nanoTime",
        descriptor = "()J",
        isStatic = true,
        isNative = true,
    )

    private fun classInitClassNameMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "initClassName",
        descriptor = "()Ljava/lang/String;",
        isStatic = false,
        isNative = true,
    )

    private fun classIsArrayMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "isArray",
        descriptor = "()Z",
        isStatic = false,
        isNative = true,
    )

    private fun classIsPrimitiveMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "isPrimitive",
        descriptor = "()Z",
        isStatic = false,
        isNative = true,
    )

    private fun classIsInterfaceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "isInterface",
        descriptor = "()Z",
        isStatic = false,
        isNative = true,
    )

    private fun classGetSuperclassMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "getSuperclass",
        descriptor = "()Ljava/lang/Class;",
        isStatic = false,
        isNative = true,
    )

    private fun throwableFillInStackTraceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Throwable",
        name = "fillInStackTrace",
        descriptor = "(I)Ljava/lang/Throwable;",
        isStatic = false,
        isNative = true,
    )

    private fun stringInternMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/String",
        name = "intern",
        descriptor = "()Ljava/lang/String;",
        isStatic = false,
        isNative = true,
    )

    private fun threadCurrentThreadMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "currentThread",
        descriptor = "()Ljava/lang/Thread;",
        isStatic = true,
        isNative = true,
    )

    private fun threadSleepMillisMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "sleep",
        descriptor = "(J)V",
        isStatic = true,
        isNative = true,
    )

    private fun threadSleepMillisNanosMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "sleep",
        descriptor = "(JI)V",
        isStatic = true,
        isNative = true,
    )

    private fun threadSleepNanos0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "sleepNanos0",
        descriptor = "(J)V",
        isStatic = true,
        isNative = true,
    )
}
