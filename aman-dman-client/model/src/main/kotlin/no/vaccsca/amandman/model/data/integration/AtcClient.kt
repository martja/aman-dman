package no.vaccsca.amandman.model.data.integration

import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientArrivalData
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientDepartureData
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.AtcClientRunwaySelectionData
import no.vaccsca.amandman.model.domain.valueobjects.atcClient.ControllerInfoData
import java.io.Closeable

interface AtcClient : Closeable {
    fun start(
        onControllerInfoData: (ControllerInfoData) -> Unit
    )

    fun collectDataFor(
        airportIcao: String,
        onArrivalsReceived: (List<AtcClientArrivalData>) -> Unit,
        onDeparturesReceived: (List<AtcClientDepartureData>) -> Unit,
        onRunwaySelectionChanged: (List<AtcClientRunwaySelectionData>) -> Unit,
    )

    fun stopCollectingMovementsFor(airportIcao: String)
    fun assignRunway(callsign: String, newRunway: String)

    override fun close()
}