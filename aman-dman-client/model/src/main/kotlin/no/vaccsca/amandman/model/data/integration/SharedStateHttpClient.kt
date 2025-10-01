package no.vaccsca.amandman.model.data.integration

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.data.dto.sharedState.SharedStateEventJson
import no.vaccsca.amandman.model.data.dto.sharedState.SharedStateJson
import no.vaccsca.amandman.model.data.repository.SettingsRepository
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.DepartureEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayArrivalEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.RunwayDelayEvent
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import no.vaccsca.amandman.model.domain.valueobjects.RunwayStatus
import no.vaccsca.amandman.model.domain.valueobjects.weather.VerticalWeatherProfile
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SharedStateHttpClient {

    private val httpClient = OkHttpClient()
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
        registerModule(KotlinxInstantModule)
        findAndRegisterModules()
    }

    val JSON = "application/json".toMediaType()
    val BASE_URL: String = SettingsRepository.getSettings(reload = true).connectionConfig.api.host

    fun sendTimelineEvents(airportIcao: String, timelineEvents: List<TimelineEvent>) {
        val events = timelineEvents.map { event ->
            val type = when (event) {
                is RunwayArrivalEvent -> "runwayArrival"
                is DepartureEvent -> "runwayDeparture"
                is RunwayDelayEvent -> "runwayDelay"
                else -> throw IllegalArgumentException("Unknown event type: ${event::class}")
            }
            SharedStateEventJson(type = type, event = event)
        }

        val sharedState = SharedStateJson(
            lastUpdate = Clock.System.now(),
            data = events
        )

        sendStateJson(airportIcao, "events", sharedState)
    }

    fun getTimelineEvents(airportIcao: String): List<TimelineEvent> {
        val typeRef = object : TypeReference<SharedStateJson<List<SharedStateEventJson>>>() {}
        val timelineEvents = fetchStateJson(airportIcao, "events", typeRef)

        return timelineEvents.data.map {
            when (it.type) {
                "runwayArrival" -> it.event as RunwayArrivalEvent
                "runwayDeparture" -> it.event as DepartureEvent
                "runwayDelay" -> it.event as RunwayDelayEvent
                else -> throw IllegalArgumentException("Unknown event type: ${it.type}")
            }
        }
    }

    fun getRunwayStatuses(airportIcao: String): Map<String, RunwayStatus> {
        val typeRef = object : TypeReference<SharedStateJson<Map<String, RunwayStatus>>>() {}
        val runwayStatuses = fetchStateJson(airportIcao, "runway-modes", typeRef)
        return runwayStatuses.data
    }

    fun sendRunwayStatuses(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>) {
        val sharedStateJson = SharedStateJson(
            lastUpdate = Clock.System.now(),
            data = runwayStatuses
        )
        sendStateJson(airportIcao, "runway-modes", sharedStateJson)
    }

    fun sendWeatherData(airportIcao: String, weatherData: VerticalWeatherProfile?) {
        val sharedStateJson = SharedStateJson(
            lastUpdate = Clock.System.now(),
            data = weatherData
        )
        sendStateJson(airportIcao, "weather", sharedStateJson)
    }

    fun getWeatherData(airportIcao: String): VerticalWeatherProfile? {
        val typeRef = object : TypeReference<SharedStateJson<VerticalWeatherProfile?>>() {}
        val weather = fetchStateJson(airportIcao, "weather", typeRef)
        return weather.data
    }

    fun getMinimumSpacing(airportIcao: String): Double {
        val typeRef = object : TypeReference<SharedStateJson<Double>>() {}
        val minimumSpacing = fetchStateJson(airportIcao, "minimum-spacing", typeRef)
        return minimumSpacing.data
    }

    fun sendMinimumSpacing(airportIcao: String, minimumSpacingNm: Double) {
        val sharedStateJson = SharedStateJson(
            lastUpdate = Clock.System.now(),
            data = minimumSpacingNm
        )
        sendStateJson(airportIcao, "minimum-spacing", sharedStateJson)
    }

    object KotlinxInstantModule : SimpleModule("KotlinxInstantModule") {
        init {
            addSerializer(Instant::class.java, object : JsonSerializer<Instant>() {
                override fun serialize(
                    value: Instant,
                    gen: JsonGenerator,
                    serializers: SerializerProvider
                ) {
                    gen.writeString(value.toString()) // ISO-8601
                }
            })
            addDeserializer(Instant::class.java, object : JsonDeserializer<Instant>() {
                override fun deserialize(
                    p: JsonParser,
                    ctxt: DeserializationContext
                ): Instant = Instant.Companion.parse(p.text)
            })
        }
    }

    // Primary fetchStateJson method that uses TypeReference for complete type safety
    private fun <T> fetchStateJson(airportIcao: String, dataType: String, typeRef: TypeReference<SharedStateJson<T>>): SharedStateJson<T> {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/airports/$airportIcao/$dataType")
            .get()
            .build()

        val response = httpClient.newCall(request).execute().use { response ->
            response.body.string()
        }

        return objectMapper.readValue(response, typeRef)
    }

    // Convenience overload for simple types (when type erasure isn't an issue)
    private inline fun <reified T> fetchStateJson(airportIcao: String, dataType: String): SharedStateJson<T> {
        val typeRef = object : TypeReference<SharedStateJson<T>>() {}
        return fetchStateJson(airportIcao, dataType, typeRef)
    }

    private fun sendStateJson(airportIcao: String, dataType: String, sharedStateJson: SharedStateJson<*>) {
        val json = objectMapper.writeValueAsString(sharedStateJson)

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/airports/$airportIcao/$dataType")
            .post(json.toRequestBody(JSON))
            .build()

        val response = httpClient.newCall(request).execute().use { response ->
            response.body.string()
        }

        println(response)
    }
}