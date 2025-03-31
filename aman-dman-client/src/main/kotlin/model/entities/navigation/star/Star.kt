package org.example.model.entities.navigation.star

import org.example.model.entities.navigation.RoutePoint

data class Star(
    val id: String,
    val airfieldElevationFt: Int,
    val fixes: List<StarFix>
)
