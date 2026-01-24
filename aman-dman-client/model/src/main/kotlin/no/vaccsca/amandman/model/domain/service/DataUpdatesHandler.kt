package no.vaccsca.amandman.model.domain.service

import kotlinx.datetime.Instant
import no.vaccsca.amandman.common.NtpClock
import no.vaccsca.amandman.model.data.integration.MasterSlaveSharedState
import no.vaccsca.amandman.model.domain.valueobjects.NonSequencedEvent
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import kotlin.time.Duration.Companion.seconds

/**
 * Responsible for notifying the presenter about data updates from the service.
 */
class GuiDataHandler : DataUpdateListener {
    lateinit var presenter: DataUpdateListener

    override fun onTimelineEventsUpdated(airportIcao: String, timelineEvents: List<TimelineEvent>) {
        presenter.onTimelineEventsUpdated(airportIcao, timelineEvents)
    }

    override fun onRunwayModesUpdated(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>) {
        presenter.onRunwayModesUpdated(airportIcao, runwayStatuses)
    }

    override fun onWeatherDataUpdated(airportIcao: String, data: VerticalWeatherProfile?) {
        presenter.onWeatherDataUpdated(airportIcao, data)
    }

    override fun onNonSequencedListUpdated(
        airportIcao: String,
        nonSequencedList: List<NonSequencedEvent>
    ) {
        presenter.onNonSequencedListUpdated(airportIcao, nonSequencedList)
    }

    override fun onMinimumSpacingUpdated(airportIcao: String, minimumSpacingNm: Double) {
        presenter.onMinimumSpacingUpdated(airportIcao, minimumSpacingNm)
    }
}

/**
 * Responsible for sending data updates to HTTP server.
 */
class DataUpdatesServerSender(
    private val masterSlaveSharedState: MasterSlaveSharedState
) : DataUpdateListener {

    val sendUpdateMem = mutableMapOf<String, Instant>()

    override fun onTimelineEventsUpdated(airportIcao: String, timelineEvents: List<TimelineEvent>) {
        val now = NtpClock.now()
        sendUpdateMem[airportIcao]
            ?.takeIf { now - it <= 2.seconds }
            ?: run {
                masterSlaveSharedState.sendTimelineEvents(airportIcao, timelineEvents)
                sendUpdateMem[airportIcao] = now
            }
    }

    override fun onRunwayModesUpdated(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>) {
        masterSlaveSharedState.sendRunwayStatuses(airportIcao, runwayStatuses)
    }

    override fun onWeatherDataUpdated(airportIcao: String, data: VerticalWeatherProfile?) {
        masterSlaveSharedState.sendWeatherData(airportIcao, data)
    }

    override fun onNonSequencedListUpdated(
        airportIcao: String,
        nonSequencedList: List<NonSequencedEvent>
    ) {
        masterSlaveSharedState.sendNonSequencedList(airportIcao, nonSequencedList)
    }

    override fun onMinimumSpacingUpdated(airportIcao: String, minimumSpacingNm: Double) {
        masterSlaveSharedState.sendMinimumSpacing(airportIcao, minimumSpacingNm)
    }
}
