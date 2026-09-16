plugins {
    `java-library`
}

dependencies {
    api(project(":libs:domain"))
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.2.0")
}
