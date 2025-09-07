package no.vaccsca.amandman.model

/**
 * Defines the operating modes for the AMAN/DMAN application
 */
enum class ApplicationMode {
    /**
     * Local mode: No network connectivity, operates independently
     */
    LOCAL,

    /**
     * Master mode: Hosts a server that shares the arrival sequence with connected slaves
     */
    MASTER,

    /**
     * Slave mode: Connects to a master server and receives the arrival sequence
     */
    SLAVE
}
