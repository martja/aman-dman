group = "no.vaccsca.amandman"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":model"))
    implementation(project(":service"))
    implementation(project(":common"))
    implementation(project(":integration"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}