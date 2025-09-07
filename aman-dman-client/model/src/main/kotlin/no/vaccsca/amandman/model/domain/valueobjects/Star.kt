package no.vaccsca.amandman.model.domain.valueobjects


data class Star(
    val id: String,
    val airport: String,
    val runway: String,
    val fixes: List<StarFix>
)
