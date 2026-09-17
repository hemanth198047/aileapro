pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "aileapro"

include("api", "worker", "executor")
include("libs:domain", "libs:security", "libs:persistence", "libs:ai")

project(":libs:domain").projectDir = file("libs/domain")
project(":libs:security").projectDir = file("libs/security")
project(":libs:persistence").projectDir = file("libs/persistence")
project(":libs:ai").projectDir = file("libs/ai")
