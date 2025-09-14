package no.vaccsca.amandman.model.domain.valueobjects.atcClient

class AtcClientRunwaySelectionData(
    val runway: String,
    val allowArrivals: Boolean,
    val allowDepartures: Boolean
)
/*

class AmanDataRetrieverPlanner(
    private val atcClient: AmanAtcClient,
    private val planner: AmanPlannerServiceMaster,
    private val dataUpdateListener: DataUpdateListener
) : AmanDataRetriever {
    override fun subscribeForArrivals(
        icao: String,
        callback: (List<RunwayArrivalEvent>) -> Unit
    ) {
        atcClient.collectMovementsFor(icao,
            onDataReceived = { arrivals ->
                planner.handleUpdateFromAtcClient(icao, arrivals)
            },
            onRunwaySelectionChanged = { runways ->
                val map = runways.associate {
                    it.runway to RunwayStatus(it.allowArrivals,it.allowDepartures)
                }
                dataUpdateListener.onRunwayModesUpdated(icao, map)
            }
        )
    }
}
*/