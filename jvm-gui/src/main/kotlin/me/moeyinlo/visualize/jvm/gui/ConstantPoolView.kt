package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFieldRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantInterfaceMethodRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantInvokeDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodHandleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantModuleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPackageEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolSlot
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry

data class ConstantPoolItem(
    val index: Int,
    val kind: String,
    val summary: String,
) {
    fun displayText(): String = "#$index = $kind $summary"
}

data class ConstantPoolModel(
    val items: List<ConstantPoolItem> = emptyList(),
) {
    companion object {
        fun fromConstantPool(constantPool: ConstantPool): ConstantPoolModel =
            ConstantPoolModel(
                (1 until constantPool.constantPoolCount).map { rawIndex ->
                    when (val slot = constantPool.slotAt(ConstantPoolIndex(rawIndex))) {
                        is ConstantPoolSlot.Entry -> slot.value.toItem(rawIndex)
                        ConstantPoolSlot.Unusable -> ConstantPoolItem(
                            index = rawIndex,
                            kind = "Unusable",
                            summary = "two-slot placeholder",
                        )
                    }
                },
            )

        private fun ConstantPoolEntry.toItem(index: Int): ConstantPoolItem =
            ConstantPoolItem(
                index = index,
                kind = kindName(),
                summary = summary(),
            )

        private fun ConstantPoolEntry.kindName(): String =
            javaClass.simpleName
                .removePrefix("Constant")
                .removeSuffix("Entry")

        private fun ConstantPoolEntry.summary(): String =
            when (this) {
                is ConstantUtf8Entry -> value
                is ConstantIntegerEntry -> value.toString()
                is ConstantFloatEntry -> value.toString()
                is ConstantLongEntry -> value.toString()
                is ConstantDoubleEntry -> value.toString()
                is ConstantClassEntry -> "name_index=$nameIndex"
                is ConstantStringEntry -> "string_index=$stringIndex"
                is ConstantNameAndTypeEntry -> "name_index=$nameIndex descriptor_index=$descriptorIndex"
                is ConstantFieldRefEntry -> "class_index=$classIndex name_and_type_index=$nameAndTypeIndex"
                is ConstantMethodRefEntry -> "class_index=$classIndex name_and_type_index=$nameAndTypeIndex"
                is ConstantInterfaceMethodRefEntry -> "class_index=$classIndex name_and_type_index=$nameAndTypeIndex"
                is ConstantMethodHandleEntry -> "reference_kind=${referenceKind.value} reference_index=$referenceIndex"
                is ConstantMethodTypeEntry -> "descriptor_index=$descriptorIndex"
                is ConstantDynamicEntry -> "bootstrap_method_attr_index=${bootstrapMethodIndex.value} name_and_type_index=$nameAndTypeIndex"
                is ConstantInvokeDynamicEntry -> "bootstrap_method_attr_index=${bootstrapMethodIndex.value} name_and_type_index=$nameAndTypeIndex"
                is ConstantModuleEntry -> "name_index=$nameIndex"
                is ConstantPackageEntry -> "name_index=$nameIndex"
                else -> toString()
            }
    }
}

object ConstantPoolViewModel {
    const val Title: String = "Constant Pool"
}

class ConstantPoolView(
    model: ConstantPoolModel = ConstantPoolModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: ConstantPoolModel) {
        items.setAll(model.items.map(ConstantPoolItem::displayText))
    }
}
