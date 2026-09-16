pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "ailepro"

include("api", "worker", "executor")
include("libs:domain", "libs:security", "libs:persistence")

project(":libs:domain").projectDir = file("libs/domain")
project(":libs:security").projectDir = file("libs/security")
project(":libs:persistence").projectDir = file("libs/persistence")
