plugins {
    application
    id("org.openjfx.javafxplugin")
}

javafx {
    version = "26.0.1"
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("me.moeyinlo.visualize.jvm.gui.VisualizeJvmApplicationKt")
}

dependencies {
    implementation(project(":jvm-classfile"))
}
