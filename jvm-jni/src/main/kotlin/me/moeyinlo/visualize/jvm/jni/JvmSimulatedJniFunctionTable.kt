package me.moeyinlo.visualize.jvm.jni

class JvmSimulatedJniFunctionTable internal constructor(
    val newLocalRef: (JvmJniHandleId?) -> JvmJniHandleId?,
    val deleteLocalRef: (JvmJniHandleId?) -> Unit,
    val newGlobalRef: (JvmJniHandleId?) -> JvmJniHandleId?,
    val deleteGlobalRef: (JvmJniHandleId?) -> Unit,
    val newWeakGlobalRef: (JvmJniHandleId?) -> JvmJniHandleId?,
    val deleteWeakGlobalRef: (JvmJniHandleId?) -> Unit,
    val getObjectRefType: (JvmJniHandleId?) -> JvmJniReferenceType,
    val isSameObject: (JvmJniHandleId?, JvmJniHandleId?) -> Boolean,
    val ensureLocalCapacity: (Int) -> Int,
    val pushLocalFrame: (Int) -> Int,
    val popLocalFrame: (JvmJniHandleId?) -> JvmJniHandleId?,
    val findClass: (String) -> JvmJniHandleId,
    val getObjectClass: (JvmJniHandleId) -> JvmJniHandleId,
    val isInstanceOf: (JvmJniHandleId?, JvmJniHandleId) -> Boolean,
    val getMethodId: (JvmJniHandleId, String, String) -> JvmJniHandleId,
    val getStaticMethodId: (JvmJniHandleId, String, String) -> JvmJniHandleId,
    val getFieldId: (JvmJniHandleId, String, String) -> JvmJniHandleId,
    val getStaticFieldId: (JvmJniHandleId, String, String) -> JvmJniHandleId,
) {
    companion object {
        fun bind(environment: JvmSimulatedJniEnvironment): JvmSimulatedJniFunctionTable =
            JvmSimulatedJniFunctionTable(
                newLocalRef = environment::newLocalRef,
                deleteLocalRef = environment::deleteLocalRef,
                newGlobalRef = environment::newGlobalRef,
                deleteGlobalRef = environment::deleteGlobalRef,
                newWeakGlobalRef = environment::newWeakGlobalRef,
                deleteWeakGlobalRef = environment::deleteWeakGlobalRef,
                getObjectRefType = environment::getObjectRefType,
                isSameObject = environment::isSameObject,
                ensureLocalCapacity = environment::ensureLocalCapacity,
                pushLocalFrame = environment::pushLocalFrame,
                popLocalFrame = environment::popLocalFrame,
                findClass = environment::findClass,
                getObjectClass = environment::getObjectClass,
                isInstanceOf = environment::isInstanceOf,
                getMethodId = environment::getMethodId,
                getStaticMethodId = environment::getStaticMethodId,
                getFieldId = environment::getFieldId,
                getStaticFieldId = environment::getStaticFieldId,
            )
    }
}
