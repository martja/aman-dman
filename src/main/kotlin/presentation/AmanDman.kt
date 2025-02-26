package org.example.presentation

import TimeRangeScrollBar
import org.example.presentation.tabpage.Footer
import org.example.presentation.tabpage.TimelineScrollPane
import org.example.presentation.tabpage.TopBar
import org.example.state.TimelineState
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.*


class AmanDman : JFrame("AMAN / DMAN") {
    private val timelineState = TimelineState()

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1000, 800)
        setLocationRelativeTo(null) // Center the window
        isAlwaysOnTop = true

        layout = BorderLayout()

        add(TimeRangeScrollBar(timelineState), BorderLayout.WEST)
        add(TimelineScrollPane(timelineState), BorderLayout.CENTER)
        add(TopBar(), BorderLayout.NORTH)
        add(Footer(), BorderLayout.SOUTH)

        isVisible = true // Show the frame

    }
}