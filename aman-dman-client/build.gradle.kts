plugins {
    kotlin("jvm") version "2.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.jtattoo:JTattoo:1.6.13")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.squareup.okio:okio:3.4.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.2")
}

tasks.test {
    useJUnitPlatform()
}