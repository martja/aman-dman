package no.vaccsca.amandman

import com.jtattoo.plaf.hifi.HiFiLookAndFeel
import no.vaccsca.amandman.controller.Controller
import no.vaccsca.amandman.integration.atcClient.AmanDataClientEuroScope
import no.vaccsca.amandman.integration.NavdataRepository
import no.vaccsca.amandman.integration.weather.WeatherDataRepository
import no.vaccsca.amandman.model.ApplicationMode
import no.vaccsca.amandman.service.AmanPlannerService
import no.vaccsca.amandman.service.DataUpdatesServerSender
import no.vaccsca.amandman.service.GuiDataHandler
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
        // First, determine the application mode
        val applicationMode = ApplicationMode.MASTER

        // --- Model ---
        val navdataRepository = NavdataRepository()
        val weatherDataRepository = WeatherDataRepository()

        // --- View ---
        val view = AmanDmanMainFrame()

        // --- Integration ---
        val atcClient = AmanDataClientEuroScope(
            host = System.getenv("ATC_HOST") ?: "127.0.0.1",
            port = System.getenv("ATC_PORT")?.toIntOrNull() ?: 12345,
        )

        // --- Service ---
        val guiUpdater = GuiDataHandler()
        val plannerService = when(applicationMode) {
            ApplicationMode.MASTER ->
                AmanPlannerService(
                    navdataRepository = navdataRepository,
                    amanDataClient = atcClient,
                    weatherDataRepository = weatherDataRepository,
                    dataUpdateListeners = arrayOf(guiUpdater, DataUpdatesServerSender()),
                )
            ApplicationMode.LOCAL ->
                AmanPlannerService(
                    navdataRepository = navdataRepository,
                    amanDataClient = atcClient,
                    weatherDataRepository = weatherDataRepository,
                    dataUpdateListeners = arrayOf(guiUpdater),
                )
            ApplicationMode.SLAVE -> null
        }

        // --- Controller ---
        val controller = Controller(plannerService, view, applicationMode)
        guiUpdater.controller = controller

        // Update window title to show network mode
        view.setWindowTitle("AMAN-DMAN - ${applicationMode.name} Mode")
        view.openWindow()

        controller.refreshWeatherData(60.0, 11.0)
    }

    // Create a new JFrame
    SwingUtilities.invokeLater {
        initializeApplication()
    }
}
