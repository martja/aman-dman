package no.vaccsca.amandman.model.data.service

class PlannerManager {
    private val services: MutableList<PlannerService> = mutableListOf()

    fun registerService(service: PlannerService) {
        services.add(service)
    }

    fun unregisterService(service: PlannerService) {
        services.remove(service)
    }

    fun getServiceForAirport(airportIcao: String): PlannerService {
        return services.find { it.airportIcao == airportIcao }!!
    }

    fun getAllServices(): List<PlannerService> {
        return services.toList()
    }
}