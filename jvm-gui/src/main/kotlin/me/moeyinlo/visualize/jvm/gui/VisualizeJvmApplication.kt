package me.moeyinlo.visualize.jvm.gui

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

object VisualizeJvmApplicationModel {
    const val Title: String = "Visualize JVM"
    const val InitialWidth: Double = 1280.0
    const val InitialHeight: Double = 800.0
}

class VisualizeJvmApplication : Application() {
    override fun start(stage: Stage) {
        stage.title = VisualizeJvmApplicationModel.Title
        stage.scene = Scene(
            VisualizeJvmRootView(),
            VisualizeJvmApplicationModel.InitialWidth,
            VisualizeJvmApplicationModel.InitialHeight,
        )
        stage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(VisualizeJvmApplication::class.java, *args)
}
