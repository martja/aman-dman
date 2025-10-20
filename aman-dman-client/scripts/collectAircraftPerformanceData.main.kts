@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.jsoup:jsoup:1.19.1")
@file:DependsOn("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.jsoup.Jsoup
import java.io.File
import kotlin.reflect.full.memberProperties

// ---------------------------
// Data class for aircraft performance
// ---------------------------
data class AircraftPerformance(
    val ICAO: String,
    val takeOffV2: Int? = null,
    val takeOffDistance: Int? = null,
    val takeOffWTC: String? = null,
    val takeOffRECAT: String? = null,
    val takeOffMTOW: Int? = null,
    val initialClimbIAS: Int? = null,
    val initialClimbROC: Int? = null,
    val climb150IAS: Int? = null,
    val climb150ROC: Int? = null,
    val climb240IAS: Int? = null,
    val climb240ROC: Int? = null,
    val machClimbMACH: Double? = null,
    val machClimbROC: Int? = null,
    val cruiseTAS: Int? = null,
    val cruiseMACH: Double? = null,
    val cruiseCeiling: Int? = null,
    val cruiseRange: Int? = null,
    val initialDescentMACH: Double? = null,
    val initialDescentROD: Int? = null,
    val descentIAS: Int? = null,
    val descentROD: Int? = null,
    val approachIAS: Int? = null,
    val approachROD: Int? = null,
    val approachMCS: Int? = null,
    val landingVat: Int? = null,
    val landingDistance: Int? = null,
    val landingAPC: String? = null
)

// ---------------------------
// Helper conversion functions
// ---------------------------
fun String?.toIntOrNullData(): Int? = when {
    this == null || this.equals("no data", true) -> null
    else -> this.toIntOrNull()
}

fun String?.toDoubleOrNullData(): Double? = when {
    this == null || this.equals("no data", true) -> null
    else -> this.toDoubleOrNull()
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
            takeOffV2 = data["takeOffV2"].toIntOrNullData(),
            takeOffDistance = data["takeOffDistance"].toIntOrNullData(),
            takeOffWTC = data["takeOffWTC"],
            takeOffRECAT = data["takeOffRECAT"],
            takeOffMTOW = data["takeOffMTOW"].toIntOrNullData(),
            initialClimbIAS = data["initialClimbIAS"].toIntOrNullData(),
            initialClimbROC = data["initialClimbROC"].toIntOrNullData(),
            climb150IAS = data["climb150IAS"].toIntOrNullData(),
            climb150ROC = data["climb150ROC"].toIntOrNullData(),
            climb240IAS = data["climb240IAS"].toIntOrNullData(),
            climb240ROC = data["climb240ROC"].toIntOrNullData(),
            machClimbMACH = data["machClimbMACH"].toDoubleOrNullData(),
            machClimbROC = data["machClimbROC"].toIntOrNullData(),
            cruiseTAS = data["cruiseTAS"].toIntOrNullData(),
            cruiseMACH = data["cruiseMACH"].toDoubleOrNullData(),
            cruiseCeiling = data["cruiseCeiling"].toIntOrNullData(),
            cruiseRange = data["cruiseRange"].toIntOrNullData(),
            initialDescentMACH = data["initialDescentMACH"].toDoubleOrNullData(),
            initialDescentROD = data["initialDescentROD"].toIntOrNullData(),
            descentIAS = data["descentIAS"].toIntOrNullData(),
            descentROD = data["descentROD"].toIntOrNullData(),
            approachIAS = data["approachIAS"].toIntOrNullData(),
            approachROD = data["approachROD"].toIntOrNullData(),
            approachMCS = data["approachMCS"].toIntOrNullData(),
            landingVat = data["landingVat"].toIntOrNullData(),
            landingDistance = data["landingDistance"].toIntOrNullData(),
            landingAPC = data["landingAPC"]
        )
}

// ---------------------------
// Main
// ---------------------------
val client = AircraftPerformanceScraper()
val availableIcaoCodes = client.getAllIcaoCodes().sorted()
val outputFile = "aircraft-performance.yaml"

// Use a Map<String, AircraftPerformanceWithoutICAO> so ICAO is the key
val allDataMap = mutableMapOf<String, Map<String, Any?>>()

for (icao in availableIcaoCodes) {
    println("Fetching $icao")
    try {
        val data = client.getPerformanceDataForIcaoCode(icao)
        val perf = client.createPerformanceDataObject(icao, data)

        // Convert data class to Map excluding ICAO
        val perfMap = perf::class.memberProperties
            .filter { it.name != "ICAO" }
            .associate { prop ->
                val value = prop.getter.call(perf)
                prop.name to value
            }

        allDataMap[icao] = perfMap
    } catch (e: Exception) {
        println("⚠️ Failed to fetch $icao: ${e.message}")
    }
    Thread.sleep(100)
}

val yamlMapper = YAMLMapper(YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)).apply {
    registerKotlinModule()
}

yamlMapper.writeValue(File(outputFile), allDataMap)

println("✅ Saved ${allDataMap.size} entries to $outputFile")