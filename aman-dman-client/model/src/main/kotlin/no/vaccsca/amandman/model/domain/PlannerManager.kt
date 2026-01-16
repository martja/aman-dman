package no.vaccsca.amandman.model.domain

import no.vaccsca.amandman.model.domain.service.PlannerService
import org.slf4j.LoggerFactory

class PlannerManager {
    private val services: MutableList<PlannerService> = mutableListOf()

    private val logger = LoggerFactory.getLogger(javaClass)

    fun registerService(service: PlannerService) {
        services.add(service)
    }

    fun unregisterService(airportIcao: String) {
        val serviceToRemove = services.find { it.airportIcao == airportIcao }
        logger.info("Unregistering service for ${serviceToRemove?.airportIcao}")
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