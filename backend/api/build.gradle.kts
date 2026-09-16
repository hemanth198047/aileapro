plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":libs:domain"))
    implementation(project(":libs:security"))
    implementation(project(":libs:persistence"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.session.data.redis)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)
    implementation(libs.springdoc)
    implementation(libs.bcprov)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testRuntimeOnly(libs.h2)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("ailepro-api.jar")
}
