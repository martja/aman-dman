repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":presenter"))
    implementation(project(":view"))
    implementation(project(":model"))
    implementation(project(":common"))

    implementation("com.jtattoo:JTattoo:1.6.13")
    implementation("ch.qos.logback:logback-classic:1.5.24")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}