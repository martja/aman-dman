package org.example.presentation

import org.example.controller.MainController
import org.example.controller.TabController
import org.example.presentation.tabpage.Footer
import org.example.state.ApplicationState
import org.example.view.TabView
import java.awt.BorderLayout
import javax.swing.*


class AmanDmanMainFrame(
    private val applicationState: ApplicationState,
    private val mainController: MainController
) : JFrame("AMAN / DMAN") {

    private val tabPane = JTabbedPane()

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1000, 800)
        setLocationRelativeTo(null) // Center the window
        isAlwaysOnTop = true

        layout = BorderLayout()

        add(tabPane, BorderLayout.CENTER)
        add(Footer(), BorderLayout.SOUTH)

        isVisible = true // Show the frame
    }

    fun addTab(name: String, tabView: TabView) {
        tabPane.addTab(name, tabView)
    }
}