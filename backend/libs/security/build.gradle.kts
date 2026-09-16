plugins {
    `java-library`
}

dependencies {
    api(project(":libs:domain"))
    implementation("org.springframework.security:spring-security-crypto:6.5.3")
    implementation(libs.bcprov)
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.springframework:spring-jcl:6.2.10")
}
