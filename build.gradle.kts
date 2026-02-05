// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.java.library)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}
dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.kotest.junit5)
    testImplementation(libs.kotest.assertions)
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(
        // Suppresses warning of dynamic agent loading used by Kotlin coroutines debugger
        "-XX:+EnableDynamicAgentLoading",
    )
}

kover {
    reports {
        verify {
            rule {
                minBound(100)
            }
        }
        filters {
            excludes {
                classes(
                    "com.buginmyhead.tools.kotlin.CollectionCoroutineExtKt",
                    "com.buginmyhead.tools.kotlin.WeakIdentityHashMap",
                    "com.buginmyhead.tools.kotlin.WeakIdentityHashMap$*",
                    "com.buginmyhead.tools.kotlin.statemachine.EffectBroker",
                    "com.buginmyhead.tools.kotlin.statemachine.EffectBroker\$DefaultImpls",
                )
            }
        }
    }
}