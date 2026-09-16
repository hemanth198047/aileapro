plugins {
    `java-library`
}

dependencies {
    api(project(":libs:domain"))
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.2.0")
    // Needed for @JdbcTypeCode so jsonb columns map correctly; version matches Spring Boot 3.5.5.
    compileOnly("org.hibernate.orm:hibernate-core:6.6.26.Final")
}
