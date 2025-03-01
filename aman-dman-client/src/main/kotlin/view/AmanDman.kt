package org.example.presentation

import org.example.controller.MainController
import org.example.presentation.tabpage.Footer
import org.example.state.ApplicationState
import org.example.view.TabView
import java.awt.BorderLayout
import javax.swing.*


class AmanDman(
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

        // Create a menubar
        val menuBar = JMenuBar().apply {
            add(JMenu("Timelines").apply {
                add(JMenuItem("Add Timeline").apply {
                    addActionListener { println("Add Timeline clicked") }
                })
                add(JMenuItem("Remove Timeline").apply {
                    addActionListener { println("Remove Timeline clicked") }
                })
                addSeparator()
                add(JMenuItem("Settings").apply {
                    addActionListener { println("Settings clicked") }
                })
            })
        }

        jMenuBar = menuBar
        isVisible = true // Show the frame
    }

    fun addTab(name: String, tabView: TabView) {
        tabPane.addTab(name, tabView)
    }
}