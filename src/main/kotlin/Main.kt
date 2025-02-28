package org.example

import com.jtattoo.plaf.hifi.HiFiLookAndFeel
import org.example.presentation.AmanDman
import javax.swing.*

fun main() {
    try {
        UIManager.setLookAndFeel("com.jtattoo.plaf.hifi.HiFiLookAndFeel")
        HiFiLookAndFeel.setTheme("Default", "", "")

       // UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel")

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
        AmanDman()
    }
}
