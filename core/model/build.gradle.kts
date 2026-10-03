// Pure Kotlin/JVM, exactly like :core:common. The domain model is the shared
// vocabulary of the editor: it must compile without Android so the rules that
// decide what an edit means stay fast to test and impossible to couple to a
// Context. Nothing in this module may import android.*.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
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
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
