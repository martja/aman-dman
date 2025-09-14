plugins {
    kotlin("jvm")
}

group = "no.vaccsca.amandman"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jfree:jfreechart:1.5.3")
    implementation(project(":common"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3") // or latest
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("edu.ucar:netcdf4:5.7.0")
    implementation("edu.ucar:cdm-core:5.7.0")
    implementation("edu.ucar:grib:5.7.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.14.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}