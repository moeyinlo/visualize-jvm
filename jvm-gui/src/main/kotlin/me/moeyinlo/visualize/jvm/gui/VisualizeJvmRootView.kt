package me.moeyinlo.visualize.jvm.gui

import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox

class VisualizeJvmRootView(
    project: JvmGuiProjectModel = JvmGuiProjectModel(),
) : BorderPane() {
    val projectClasspathPanel: ProjectClasspathPanel = ProjectClasspathPanel(project)
    val classTreeView: ClassTreeView = ClassTreeView(ClassTreeModel.fromClasspathEntries(project.classpathEntries))
    val memberListView: MemberListView = MemberListView()
    val bytecodeInstructionView: BytecodeInstructionView = BytecodeInstructionView()
    val constantPoolView: ConstantPoolView = ConstantPoolView()
    val debuggerControlBar: DebuggerControlBar = DebuggerControlBar()
    val currentFrameView: CurrentFrameView = CurrentFrameView()
    val localVariablesView: LocalVariablesView = LocalVariablesView()
    val operandStackView: OperandStackView = OperandStackView()
    val classLoadingEventsView: ClassLoadingEventsView = ClassLoadingEventsView()
    val linkingEventsView: LinkingEventsView = LinkingEventsView()

    init {
        top = VBox(debuggerControlBar, constantPoolView)
        left = projectClasspathPanel
        center = VBox(classTreeView, currentFrameView, localVariablesView, operandStackView)
        right = VBox(memberListView, classLoadingEventsView, linkingEventsView)
        bottom = bytecodeInstructionView
    }
}
