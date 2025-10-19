package no.vaccsca.amandman.model.domain.valueobjects

interface Flight {
    val callsign: String
    val icaoType: String
    val wakeCategory: Char
    val trackingController: String?
}