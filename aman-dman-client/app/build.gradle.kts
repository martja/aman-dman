group = "org.example"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":controller"))
    implementation(project(":view"))
    implementation(project(":model"))
    implementation(project(":service"))
    implementation(project(":common"))

    implementation("com.jtattoo:JTattoo:1.6.13")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}