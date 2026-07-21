package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.ListView
import me.moeyinlo.visualize.jvm.classfile.ClassFile
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry

enum class MemberKind {
    Field,
    Method,
}

data class MemberListItem(
    val kind: MemberKind,
    val name: String,
    val descriptor: String,
) {
    fun displayText(): String = "${kind.name.lowercase()} $name:$descriptor"
}

data class MemberListModel(
    val items: List<MemberListItem> = emptyList(),
) {
    companion object {
        fun fromClassFile(classFile: ClassFile): MemberListModel {
            val fields = classFile.fields.map { field ->
                MemberListItem(
                    kind = MemberKind.Field,
                    name = classFile.utf8(field.nameIndex),
                    descriptor = classFile.utf8(field.descriptorIndex),
                )
            }
            val methods = classFile.methods.map { method ->
                MemberListItem(
                    kind = MemberKind.Method,
                    name = classFile.utf8(method.nameIndex),
                    descriptor = classFile.utf8(method.descriptorIndex),
                )
            }
            return MemberListModel(fields + methods)
        }

        private fun ClassFile.utf8(index: ConstantPoolIndex): String =
            (constantPool[index] as ConstantUtf8Entry).value
    }
}

object MemberListViewModel {
    const val Title: String = "Members"
}

class MemberListView(
    model: MemberListModel = MemberListModel(),
) : ListView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: MemberListModel) {
        items.setAll(model.items.map(MemberListItem::displayText))
    }
}
