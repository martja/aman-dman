plugins {
    kotlin("jvm") version "2.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.jtattoo:JTattoo:1.6.13")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.squareup.okio:okio:3.4.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.2")
    implementation("edu.ucar:cdm-core:5.7.0")
    implementation("edu.ucar:netcdf4:5.7.0")
    implementation("edu.ucar:grib:5.7.0")
    implementation("com.google.guava:guava:32.1.2-jre")
    implementation("org.jsoup:jsoup:1.19.1")
    runtimeOnly("org.slf4j:slf4j-jdk14:1.7.32")
}

tasks.test {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    }
}