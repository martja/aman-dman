package no.vaccsca.amandman.model.domain.valueobjects

data class VersionCompatibilityResult(
    val isCompatible: Boolean,
    val requiredVersion: String,
    val newestVersion: String,
    val currentVersion: String,
)