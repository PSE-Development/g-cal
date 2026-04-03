import io.gitlab.arturbosch.detekt.Detekt

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
    jacoco
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

kotlin {
    jvmToolchain(21)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/config/detekt.yml")
    baseline = file("$projectDir/config/baseline.xml")
}


tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        sarif.required.set(true)
    }
}

group = "g-cal.server"
version = "1.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val ktor_version = "2.3.7"
val kotlinx_serialization_version = "1.6.0"

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:${ktor_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")

    implementation("org.ktorm:ktorm-core:3.6.0")
    implementation("org.postgresql:postgresql:42.7.3")

    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.apache.logging.log4j:log4j-api:2.22.1")
    implementation("org.apache.logging.log4j:log4j-core:2.22.1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.22.1")
    implementation("com.auth0:java-jwt:4.4.0")


    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.ktor:ktor-client-core:${ktor_version}")
    testImplementation("io.ktor:ktor-client-cio:${ktor_version}")
    testImplementation("io.ktor:ktor-client-content-negotiation:${ktor_version}")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${ktor_version}")
    testImplementation("com.h2database:h2:2.2.220")
    testImplementation("uk.org.webcompere:system-stubs-jupiter:2.1.8")
    testImplementation("io.ktor:ktor-server-test-host:${ktor_version}")
}

application {
    mainClass.set("controller.RequestControllerKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Builds an executable FatJar, containing all dependencies."
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "main.kotlin.controller.RequestControllerKt"
    }
    from(sourceSets.main.get().output)
    from(sourceSets.main.get().resources)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}
