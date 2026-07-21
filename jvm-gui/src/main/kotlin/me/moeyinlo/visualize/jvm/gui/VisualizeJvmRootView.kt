package me.moeyinlo.visualize.jvm.gui

import javafx.scene.layout.BorderPane

class VisualizeJvmRootView(
    project: JvmGuiProjectModel = JvmGuiProjectModel(),
) : BorderPane() {
    val projectClasspathPanel: ProjectClasspathPanel = ProjectClasspathPanel(project)
    val classTreeView: ClassTreeView = ClassTreeView(ClassTreeModel.fromClasspathEntries(project.classpathEntries))
    val memberListView: MemberListView = MemberListView()
    val bytecodeInstructionView: BytecodeInstructionView = BytecodeInstructionView()
    val constantPoolView: ConstantPoolView = ConstantPoolView()

    init {
        top = constantPoolView
        left = projectClasspathPanel
        center = classTreeView
        right = memberListView
        bottom = bytecodeInstructionView
    }
}
