// Pure Kotlin/JVM. No Android dependencies by design: the rules in here are
// shared by the editor engine, the tests, and (later) any server-side or
// desktop tooling. Nothing in this module may import android.*.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

// No jvmToolchain(...) on purpose -- provisioning a JDK 17 toolchain would
// trigger a download. We compile with the installed JDK 21 and emit 17
// bytecode, which matches the Android modules' source/target compatibility.
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
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
