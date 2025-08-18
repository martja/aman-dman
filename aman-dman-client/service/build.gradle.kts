plugins {
    kotlin("jvm")
}

group = "no.vaccsca.amandman"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.14.2")

    implementation(project(":common"))
    implementation(project(":integration"))
    implementation(project(":model"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}