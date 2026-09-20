plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":libs:domain"))
    implementation(project(":libs:security"))
    implementation(project(":libs:persistence"))
    implementation(project(":libs:ai"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.session.data.redis)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)
    implementation(libs.springdoc)
    implementation(libs.bcprov)
    // Seed content is authored as YAML, so Jackson needs the YAML backend.
    implementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testRuntimeOnly(libs.h2)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("aileapro-api.jar")
}

// Operator CLI: loads the authored curriculum files, then exits. No web server, no scheduler.
tasks.register<org.springframework.boot.gradle.tasks.run.BootRun>("seed") {
    group = "application"
    description = "Loads the authored curriculum seed into the database."
    mainClass.set("com.aileapro.api.AileaproApiApplication")
    classpath = sourceSets["main"].runtimeClasspath
    args(
        "--aileapro.seed.run=true",
        "--spring.main.web-application-type=none",
    )
}
