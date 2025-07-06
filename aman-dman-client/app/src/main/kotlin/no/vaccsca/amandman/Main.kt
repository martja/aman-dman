package no.vaccsca.amandman

import com.jtattoo.plaf.hifi.HiFiLookAndFeel
import no.vaccsca.amandman.controller.Controller
import no.vaccsca.amandman.integration.atcClient.AtcClientEuroScope
import no.vaccsca.amandman.model.AmanDmanSequence
import no.vaccsca.amandman.model.NavdataRepository
import no.vaccsca.amandman.model.WeatherDataRepository
import no.vaccsca.amandman.service.AmanDataService
import no.vaccsca.amandman.view.AmanDmanMainFrame
import java.util.*
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException


fun main() {

    // Disable hardware acceleration to prevent artifacts with popup menus
    System.setProperty("sun.java2d.d3d", "false")

    try {
        HiFiLookAndFeel.setCurrentTheme(Properties().apply {
            put("logoString", "") // Removes "JTattoo"-attribution from all popup menus
        })
        UIManager.setLookAndFeel("com.jtattoo.plaf.hifi.HiFiLookAndFeel")
        //UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel")

    } catch (e: ClassNotFoundException) {
        e.printStackTrace()
    } catch (e: InstantiationException) {
        e.printStackTrace()
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
    } catch (e: UnsupportedLookAndFeelException) {
        e.printStackTrace()
    }

    fun initializeApplication() {
        // --- Model ---
        val navdataRepository = NavdataRepository()
        val sequence = AmanDmanSequence()
        val weatherDataRepository = WeatherDataRepository()

        // --- View ---
        val view = AmanDmanMainFrame()

        // --- Integration ---
        val atcClient = AtcClientEuroScope(
            host = System.getenv("ATC_HOST") ?: "127.0.0.1",
            port = System.getenv("ATC_PORT")?.toIntOrNull() ?: 12345,
        )

        // --- Service ---
        val service = AmanDataService(sequence, navdataRepository, atcClient)

        // --- Controller ---
        val controller = Controller(service, view, weatherDataRepository)

        view.openWindow()

        controller.refreshWeatherData(60.0, 11.0)
    }

    // Create a new JFrame
    SwingUtilities.invokeLater {
        initializeApplication()
    }
}
