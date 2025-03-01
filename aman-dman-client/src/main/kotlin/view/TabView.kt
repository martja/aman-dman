package org.example.view

import TimeRangeScrollBar
import model.entities.TimelineConfig
import org.example.controller.TabController
import org.example.controller.TimelineController
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

    val timelineScrollPane = TimelineScrollPane(tabController, applicationState)

    init {
        add(TopBar(), BorderLayout.NORTH)

        add(TimeRangeScrollBar(tabController, applicationState), BorderLayout.WEST)
        add(timelineScrollPane, BorderLayout.CENTER)
    }

    fun addTimeline(timelineConfig: TimelineConfig, timelineState: TimelineState, timelineController: TimelineController) {
        timelineScrollPane.insertTimeline(timelineConfig, timelineState, timelineController)
    }
}