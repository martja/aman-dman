plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "aman-dman"

include(
    ":app",
    ":view",
    ":controller",
    ":model",
    ":service",
    ":common",
    ":integration"
)
findProject(":model:integration")?.name = "integration"
