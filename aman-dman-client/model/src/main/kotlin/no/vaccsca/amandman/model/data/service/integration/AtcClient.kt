package no.vaccsca.amandman.model.data.service.integration

import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientArrivalData
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientRunwaySelectionData
import java.io.Closeable

interface AtcClient : Closeable {
    fun collectMovementsFor(
        airportIcao: String,
        onDataReceived: (List<AtcClientArrivalData>) -> Unit,
        onRunwaySelectionChanged: (List<AtcClientRunwaySelectionData>) -> Unit
    )

    fun stopCollectingMovementsFor(airportIcao: String)

    override fun close()
}