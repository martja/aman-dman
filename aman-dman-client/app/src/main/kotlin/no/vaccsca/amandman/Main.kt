package no.vaccsca.amandman

import com.jtattoo.plaf.hifi.HiFiLookAndFeel
import no.vaccsca.amandman.presenter.Presenter
import no.vaccsca.amandman.model.data.repository.NavdataRepository
import no.vaccsca.amandman.model.ApplicationMode
import no.vaccsca.amandman.model.data.repository.WeatherDataRepository
import no.vaccsca.amandman.model.data.service.integration.AtcClientEuroScope
import no.vaccsca.amandman.model.data.service.PlannerServiceMaster
import no.vaccsca.amandman.model.data.service.PlannerServiceSlave
import no.vaccsca.amandman.model.data.service.integration.SharedStateHttpClient
import no.vaccsca.amandman.model.domain.service.DataUpdatesServerSender
import no.vaccsca.amandman.model.domain.service.GuiDataHandler
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
        val applicationMode = ApplicationMode.LOCAL

        // --- View ---
        val view = AmanDmanMainFrame()

        val host = System.getenv("ATC_HOST") ?: "127.0.0.1"
        val port = System.getenv("ATC_PORT")?.toIntOrNull() ?: 12345

        // --- Service ---
        val guiUpdater = GuiDataHandler()
        val plannerService = when(applicationMode) {
            ApplicationMode.MASTER ->
                PlannerServiceMaster(
                    weatherDataRepository = WeatherDataRepository(),
                    atcClient = AtcClientEuroScope(host, port),
                    navdataRepository = NavdataRepository(),
                    dataUpdateListeners = arrayOf(guiUpdater, DataUpdatesServerSender()),
                )
            ApplicationMode.LOCAL ->
                PlannerServiceMaster(
                    weatherDataRepository = WeatherDataRepository(),
                    atcClient = AtcClientEuroScope(host, port),
                    navdataRepository = NavdataRepository(),
                    dataUpdateListeners = arrayOf(guiUpdater),
                )
            ApplicationMode.SLAVE ->
                PlannerServiceSlave(
                    sharedStateHttpClient = SharedStateHttpClient(),
                    dataUpdateListener = guiUpdater,
                )
        }

        // --- Controller ---
        val presenter = Presenter(plannerService, view, applicationMode)
        guiUpdater.presenter = presenter

        // Update window title to show network mode
        view.setWindowTitle("AMAN-DMAN - ${applicationMode.name} Mode")
        view.openWindow()

        presenter.refreshWeatherData("ENGM", 60.0, 11.0)
    }

    // Create a new JFrame
    SwingUtilities.invokeLater {
        initializeApplication()
    }
}
