package org.example

import AmanDmanMainFrame
import Controller
import com.jtattoo.plaf.hifi.HiFiLookAndFeel
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

    // Create a new JFrame
    SwingUtilities.invokeLater {
        val view = AmanDmanMainFrame()
        val model = AmanDataService()
        val controller = Controller(model, view)

        model.livedataInferface = controller
        view.openWindow(controller)

        controller.refreshWeatherData(60.0, 11.0)
    }
}
