@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.jsoup:jsoup:1.19.1")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

import kotlinx.serialization.encodeToString
import java.io.File
import org.jsoup.Jsoup
import kotlinx.serialization.json.*
import kotlin.reflect.full.memberProperties

// ---------------------------
// Data class for aircraft performance
// ---------------------------
data class AircraftPerformance(
    val ICAO: String,
    val takeOffV2: String? = null,
    val takeOffDistance: String? = null,
    val takeOffWTC: String? = null,
    val takeOffRECAT: String? = null,
    val takeOffMTOW: String? = null,
    val initialClimbIAS: String? = null,
    val initialClimbROC: String? = null,
    val climb150IAS: String? = null,
    val climb150ROC: String? = null,
    val climb240IAS: String? = null,
    val climb240ROC: String? = null,
    val machClimbMACH: String? = null,
    val machClimbROC: String? = null,
    val cruiseTAS: String? = null,
    val cruiseMACH: String? = null,
    val cruiseCeiling: String? = null,
    val cruiseRange: String? = null,
    val initialDescentMACH: String? = null,
    val initialDescentROD: String? = null,
    val descentIAS: String? = null,
    val descentROD: String? = null,
    val approachIAS: String? = null,
    val approachROD: String? = null,
    val approachMCS: String? = null,
    val landingVat: String? = null,
    val landingDistance: String? = null,
    val landingAPC: String? = null
)

// ---------------------------
// Helper: convert data class to JsonObject
// Skips nulls and "no data", converts numeric strings to numbers
// ---------------------------
fun Any.toJsonObject(): JsonObject {
    val map = this::class.memberProperties.mapNotNull { prop ->
        val value = prop.getter.call(this)?.toString()?.trim() ?: return@mapNotNull null

        val jsonValue = when {
            value.equals("no data", ignoreCase = true) -> JsonPrimitive(null)
            value.matches(Regex("^-?\\d+$")) -> JsonPrimitive(value.toInt())
            value.matches(Regex("^-?\\d+\\.\\d+$")) -> JsonPrimitive(value.toDouble())
            else -> JsonPrimitive(value)
        }

        prop.name to jsonValue
    }.toMap()

    return JsonObject(map)
}

// ---------------------------
// Scraper
// ---------------------------
class AircraftPerformanceScraper {
    private val baseUrl = "https://learningzone.eurocontrol.int/ilp/customs/ATCPFDB/details.aspx"

    fun getAllIcaoCodes(): List<String> {
        val doc = Jsoup.connect("$baseUrl?ICAO=").get()
        return doc.select(".ap-dropdown option")
            .map { it.attr("value") }
            .filter { it.isNotEmpty() }
    }

    fun getPerformanceDataForIcaoCode(icaoCode: String): Map<String, String> {
        val url = "$baseUrl?ICAO=$icaoCode"
        val doc = Jsoup.connect(url).get()
        return doc.select("span.ap-list-item-perf-value")
            .associate {
                val key = it.attr("datagraph").trim()
                val value = it.text().trim()
                key to value
            }
            .filter { it.key.isNotEmpty() && it.value.isNotEmpty() }
    }

    fun createPerformanceDataObject(icaoCode: String, data: Map<String, String>) =
        AircraftPerformance(
            ICAO = icaoCode,
            takeOffV2 = data["takeOffV2"],
            takeOffDistance = data["takeOffDistance"],
            takeOffWTC = data["takeOffWTC"],
            takeOffRECAT = data["takeOffRECAT"],
            takeOffMTOW = data["takeOffMTOW"],
            initialClimbIAS = data["initialClimbIAS"],
            initialClimbROC = data["initialClimbROC"],
            climb150IAS = data["climb150IAS"],
            climb150ROC = data["climb150ROC"],
            climb240IAS = data["climb240IAS"],
            climb240ROC = data["climb240ROC"],
            machClimbMACH = data["machClimbMACH"],
            machClimbROC = data["machClimbROC"],
            cruiseTAS = data["cruiseTAS"],
            cruiseMACH = data["cruiseMACH"],
            cruiseCeiling = data["cruiseCeiling"],
            cruiseRange = data["cruiseRange"],
            initialDescentMACH = data["initialDescentMACH"],
            initialDescentROD = data["initialDescentROD"],
            descentIAS = data["descentIAS"],
            descentROD = data["descentROD"],
            approachIAS = data["approachIAS"],
            approachROD = data["approachROD"],
            approachMCS = data["approachMCS"],
            landingVat = data["landingVat"],
            landingDistance = data["landingDistance"],
            landingAPC = data["landingAPC"]
        )
}

// ---------------------------
// Main
// ---------------------------
val client = AircraftPerformanceScraper()
val availableIcaoCodes = client.getAllIcaoCodes().sorted()
val outputFile = "aircraft_performance.json"

val allData = buildJsonArray {
    for (icao in availableIcaoCodes) {
        println("Fetching $icao")
        try {
            val data = client.getPerformanceDataForIcaoCode(icao)
            val perf = client.createPerformanceDataObject(icao, data)
            add(perf.toJsonObject())
        } catch (e: Exception) {
            println("⚠️ Failed to fetch $icao: ${e.message}")
        }
        Thread.sleep(100) // polite delay
    }
}

// Write JSON
File(outputFile).writeText(Json { prettyPrint = true }.encodeToString(allData))
println("✅ Saved ${allData.size} entries to $outputFile")
