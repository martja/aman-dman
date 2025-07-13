package no.vaccsca.amandman.model

interface Flight {
    val callsign: String
    val icaoType: String
    val wakeCategory: Char
}