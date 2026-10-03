// Pure Kotlin/JVM. This is the heart of the editor: timeline maths, the edit
// commands, and undo/redo. It is deliberately free of Android so the rules that
// decide what an edit *means* are synchronous, deterministic, and testable
// without a device or an emulator. Nothing here may import android.*.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

// See :core:common -- no jvmToolchain(...) on purpose, to avoid provisioning a
// JDK 17 download. We compile on the installed JDK 21 and emit 17 bytecode.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
