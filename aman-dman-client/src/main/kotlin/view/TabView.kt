package org.example.view

import TimeRangeScrollBar
import model.entities.TimelineConfig
import org.example.controller.TabController
import org.example.model.TabState
import org.example.model.TimelineState
import org.example.presentation.tabpage.TimelineScrollPane
import org.example.presentation.tabpage.TopBar
import java.awt.BorderLayout
import javax.swing.JPanel

class TabView(
    private val tabController: TabController,
    private val applicationState: TabState
) : JPanel(BorderLayout()) {

    val timeWindowScrollbar = TimeRangeScrollBar(tabController, applicationState)
    val timelineScrollPane = TimelineScrollPane(tabController, applicationState)

    init {
        add(TopBar(), BorderLayout.NORTH)

        add(timeWindowScrollbar, BorderLayout.WEST)
        add(timelineScrollPane, BorderLayout.CENTER)

    }

    fun addTimeline(timelineConfig: TimelineConfig, timelineState: TimelineState) {
        timelineScrollPane.insertTimeline(timelineConfig, timelineState)
    }
}