package no.vaccsca.amandman.model.navigation.star


data class Star(
    val id: String,
    val airport: String,
    val runway: String,
    val fixes: List<StarFix>
)
