package no.vaccsca.amandman.model.domain.exception

data class UnsupportedInSlaveModeException(val msg: String) : Exception(msg)
