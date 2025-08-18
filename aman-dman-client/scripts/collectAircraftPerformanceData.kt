package integration

import org.jsoup.Jsoup
import java.io.File

fun main() {
    val client = AircraftPerformanceScraper()
    val availableIcaoCodes = client.getAllIcaoCodes().reversed().takeWhile { it != "B74D" }.reversed()
    val outputFile = "aircraft_performance.csv"

    // Write CSV headers
    val headers = listOf("ICAO", "takeOffV2", "takeOffDistance", "takeOffWTC", "takeOffRECAT", "takeOffMTOW", "initialClimbIAS", "initialClimbROC", "climb150IAS", "climb150ROC", "climb240IAS", "climb240ROC", "machClimbMACH", "machClimbROC", "cruiseTAS", "cruiseMACH", "cruiseCeiling", "cruiseRange", "initialDescentMACH", "initialDescentROD", "descentIAS", "descentROD", "approachIAS", "approachROD", "approachMCS", "landingVat", "landingDistance", "landingAPC")
    File(outputFile).printWriter().use { out ->
        out.println(headers.joinToString(","))
    }

    availableIcaoCodes.forEach { icaoCode ->
        Thread.sleep(1000) // Avoid overwhelming the server
        val performanceData = client.getPerformanceDataForIcaoCode(icaoCode)
        client.appendPerformanceDataToCsv(icaoCode, performanceData, outputFile)
        println(icaoCode)
    }
}

class AircraftPerformanceScraper {
    private val baseUrl = "https://contentzone.eurocontrol.int/aircraftperformance/details.aspx"

    fun getAllIcaoCodes(): List<String> {
        val doc = Jsoup.connect("$baseUrl?ICAO=").get()
        return doc.select(".ap-dropdown option").map { it.attr("value") }.filter { it.isNotEmpty() }
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

    fun appendPerformanceDataToCsv(icaoCode: String, performanceData: Map<String, String>, outputFileName: String) {
        val defaultKeys = listOf("takeOffV2", "takeOffDistance", "takeOffWTC", "takeOffRECAT", "takeOffMTOW", "initialClimbIAS", "initialClimbROC", "climb150IAS", "climb150ROC", "climb240IAS", "climb240ROC", "machClimbMACH", "machClimbROC", "cruiseTAS", "cruiseMACH", "cruiseCeiling", "cruiseRange", "initialDescentMACH", "initialDescentROD", "descentIAS", "descentROD", "approachIAS", "approachROD", "approachMCS", "landingVat", "landingDistance", "landingAPC")

        val row = listOf(icaoCode) + defaultKeys.map { performanceData[it] ?: "" }

        File(outputFileName).appendText(row.joinToString(",") + "\n")
    }
}
