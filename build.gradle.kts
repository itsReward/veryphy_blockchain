plugins {
    kotlin("jvm") version "1.8.10"
    id("org.jetbrains.kotlin.plugin.spring") version "1.8.10"
    id("org.springframework.boot") version "3.1.0"
    id("io.spring.dependency-management") version "1.1.0"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.8.10"
    application
}

group = "com.veryphy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    // Hyperledger Fabric repositories
    maven { url = uri("https://hyperledger.jfrog.io/hyperledger/fabric-maven") }
    maven { url = uri("https://hyperledger.jfrog.io/hyperledger/fabric-gateway-maven") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Database
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.h2database:h2:2.2.220") // For development/testing
    implementation("org.flywaydb:flyway-core:9.16.3") // For database migrations

    // Hyperledger Fabric SDK
    implementation("org.hyperledger.fabric:fabric-gateway-java:2.2.0")
    implementation("org.hyperledger.fabric-sdk-java:fabric-sdk-java:2.2.0")
    implementation("org.hyperledger.fabric-chaincode-java:fabric-chaincode-shim:2.2.0")
    //implementation("org.hyperledger.fabric:fabric-contract-api:2.2.0")

    // AI & Image Processing
    implementation("org.tensorflow:tensorflow-core-platform:0.5.0")
    implementation("org.tensorflow:tensorflow-framework:0.5.0")
    implementation("org.deeplearning4j:deeplearning4j-core:1.0.0-M2.1")
    implementation("org.bytedeco:javacv-platform:1.5.9")

    // PDF Generation
    implementation("com.itextpdf:itext7-core:7.2.5")

    // Utility Libraries
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-io:commons-io:2.13.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.7")

    //Json
    implementation("io.jsonwebtoken:jjwt-api:0.11.5") // Or the latest version
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5") // If you are using Jackson for JSON processing

    // Swagger/OpenAPI 3.0 dependencies
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
    implementation("org.springdoc:springdoc-openapi-starter-common:2.2.0")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.7.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass.set("com.veryphy.ApplicationKt")
}