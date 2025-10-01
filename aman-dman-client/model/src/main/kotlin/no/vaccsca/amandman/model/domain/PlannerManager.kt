package no.vaccsca.amandman.model.domain

import no.vaccsca.amandman.model.domain.service.PlannerService

class PlannerManager {
    private val services: MutableList<PlannerService> = mutableListOf()

    fun registerService(service: PlannerService) {
        services.add(service)
    }

    fun unregisterService(airportIcao: String) {
        val serviceToRemove = services.find { it.airportIcao == airportIcao }
        println("Unregistering service for $serviceToRemove")
        serviceToRemove?.stop()
        services.remove(serviceToRemove)
    }

    fun getServiceForAirport(airportIcao: String): PlannerService {
        return services.find { it.airportIcao == airportIcao }!!
    }

    fun getAllServices(): List<PlannerService> {
        return services.toList()
    }
}