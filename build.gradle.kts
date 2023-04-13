group = "dev.storozhenko"
version = "2.0.0"
description = "familybot"
java.sourceCompatibility = JavaVersion.VERSION_17
sourceSets.main {
    java.srcDirs("src/main/java", "src/main/kotlin")
}

plugins {
    kotlin("jvm") version "1.8.10"
    id("org.jetbrains.kotlin.plugin.spring") version ("1.8.10")
    id("org.springframework.boot") version ("2.4.5")
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.0.4"))
    implementation("org.springframework.boot:spring-boot-starter-jdbc:3.0.4")
    implementation("org.springframework.boot:spring-boot-starter-web:3.0.4")
    implementation("org.springframework.data:spring-data-redis:3.0.4")
    implementation("org.springframework.boot:spring-boot-starter-actuator:3.0.4")
    implementation("org.springframework.boot:spring-boot-configuration-processor:3.0.4")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation("org.postgresql:postgresql:42.5.4")
    implementation("io.micrometer:micrometer-core:1.10.5")
    implementation("io.micrometer:micrometer-registry-graphite:1.10.5")
    implementation("io.lettuce:lettuce-core:6.2.3.RELEASE")
    implementation("org.aspectj:aspectjweaver:1.9.19")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.2")
    implementation("org.telegram:telegrambots:6.5.0")
    implementation("org.tomlj:tomlj:1.1.0")

    implementation("com.theokanning.openai-gpt3-java:api:0.11.0")
    implementation("com.theokanning.openai-gpt3-java:client:0.11.0")
    implementation("com.theokanning.openai-gpt3-java:service:0.11.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.0.4")
    testImplementation("org.testcontainers:testcontainers:1.17.6")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.8.10")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}
