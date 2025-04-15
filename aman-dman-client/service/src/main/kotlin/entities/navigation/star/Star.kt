package org.example.entities.navigation.star


data class Star(
    val id: String,
    val airport: String,
    val runway: String,
    val fixes: List<StarFix>
)
