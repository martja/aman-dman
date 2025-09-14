package no.vaccsca.amandman.model.data.service.integration

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

    val BASE_URL = "https://aman-dman-api.fly.dev"
    val JSON = "application/json".toMediaType()

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

        val json = objectMapper.writeValueAsString(sharedState)

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/airports/$airportIcao/events")
            .post(json.toRequestBody(JSON))
            .build()

        val response = httpClient.newCall(request).execute().use { it.body!!.string() }

        println(response)
    }

    fun getTimelineEvents(airportIcao: String): List<TimelineEvent> {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/airports/$airportIcao/events")
            .get()
            .build()

        val response = httpClient.newCall(request).execute().use { response ->
            response.body.string()
        }

        val state = objectMapper.readValue(response, object : TypeReference<SharedStateJson<List<SharedStateEventJson>>>() {})

        return state.data.map {
            when (it.type) {
                "runwayArrival" -> it.event as RunwayArrivalEvent
                "runwayDeparture" -> it.event as DepartureEvent
                "runwayDelay" -> it.event as RunwayDelayEvent
                else -> throw IllegalArgumentException("Unknown event type: ${it.type}")
            }
        }
    }

    fun getRunwayStatuses(airportIcao: String): Map<String, RunwayStatus> {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/airports/$airportIcao/runway-modes")
            .get()
            .build()

        val response = httpClient.newCall(request).execute().use { response ->
            response.body.string()
        }

        val state = objectMapper.readValue(response, object : TypeReference<SharedStateJson<Map<String, RunwayStatus>>>() {})

        return state.data
    }

    fun sendRunwayStatuses(airportIcao: String, runwayStatuses: Map<String, RunwayStatus>) {
        val sharedStateJson = SharedStateJson(
            lastUpdate = Clock.System.now(),
            data = runwayStatuses
        )

        val json = objectMapper.writeValueAsString(sharedStateJson)

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/airports/$airportIcao/runway-modes")
            .post(json.toRequestBody(JSON))
            .build()

        val response = httpClient.newCall(request).execute().use { response ->
            response.body.string()
        }

        println(response)
    }

    fun sendWeatherData(airportIcao: String, weatherData: VerticalWeatherProfile?) {
        val sharedStateJson = SharedStateJson(
            lastUpdate = Clock.System.now(),
            data = weatherData
        )
        val json = objectMapper.writeValueAsString(sharedStateJson)

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/airports/$airportIcao/weather")
            .post(json.toRequestBody(JSON))
            .build()

        val response = httpClient.newCall(request).execute().use { response ->
            response.body.string()
        }

        println(response)
    }

    fun getWeatherData(airportIcao: String): VerticalWeatherProfile? {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/airports/$airportIcao/weather")
            .get()
            .build()

        val response = httpClient.newCall(request).execute().use { response ->
            response.body.string()
        }

        val state = objectMapper.readValue(response, object : TypeReference<SharedStateJson<VerticalWeatherProfile?>>() {})

        return state.data
    }

    fun getMinimumSpacing(airportIcao: String): Double {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/airports/$airportIcao/minimum-spacing")
            .get()
            .build()

        val response = httpClient.newCall(request).execute().use { response ->
            response.body.string()
        }

        val state = objectMapper.readValue(response, object : TypeReference<SharedStateJson<Double>>() {})

        return state.data
    }

    fun sendMinimumSpacing(airportIcao: String, minimumSpacingNm: Double) {
        val sharedStateJson = SharedStateJson(
            lastUpdate = Clock.System.now(),
            data = minimumSpacingNm
        )

        val json = objectMapper.writeValueAsString(sharedStateJson)
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/airports/$airportIcao/minimum-spacing")
            .post(json.toRequestBody(JSON))
            .build()

        val response = httpClient.newCall(request).execute().use { response ->
            response.body.string()
        }

        println(response)
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
}