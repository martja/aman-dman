package org.example.model.entities.navigation.star


data class Star(
    val id: String,
    val airfieldElevationFt: Int,
    val fixes: List<StarFix>
)
