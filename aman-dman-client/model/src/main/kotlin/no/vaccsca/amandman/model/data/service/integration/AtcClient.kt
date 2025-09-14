package no.vaccsca.amandman.model.data.service.integration

import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientArrivalData
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientRunwaySelectionData

interface AtcClient {
    fun collectMovementsFor(
        airportIcao: String,
        onDataReceived: (List<AtcClientArrivalData>) -> Unit,
        onRunwaySelectionChanged: (List<AtcClientRunwaySelectionData>) -> Unit
    )
}