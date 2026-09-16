import org.gradle.api.plugins.JavaPluginExtension

plugins {
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.spring.boot) apply false
}

subprojects {
    apply(plugin = "java")
    group = "com.aileapro"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
