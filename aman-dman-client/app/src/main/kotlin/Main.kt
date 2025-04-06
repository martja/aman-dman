package org.example

import AmanDmanMainFrame
import Controller
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException

fun main() {

    // Disable hardware acceleration to prevent artifacts with popup menus
    System.setProperty("sun.java2d.d3d", "false")

    try {
        UIManager.setLookAndFeel("com.jtattoo.plaf.hifi.HiFiLookAndFeel")
        //HiFiLookAndFeel.setTheme("Default", "", "")

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

        model.amanDataListener = controller
        view.openWindow(controller)

    }
}
