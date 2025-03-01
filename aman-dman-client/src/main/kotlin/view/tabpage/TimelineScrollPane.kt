package org.example.presentation.tabpage

import model.entities.TimelineConfig
import org.example.controller.TabController
import org.example.controller.TimelineController
import org.example.model.TabState
import org.example.model.TimelineState
import org.example.presentation.tabpage.timeline.TimelineView
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane


class TimelineScrollPane(
    val tabController: TabController,
    val applicationState: TabState
) : JScrollPane(VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
    init {
        val items = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.VERTICAL
        viewport.add(items)
    }

    fun insertTimeline(timelineConfig: TimelineConfig, timelineState: TimelineState, timelineController: TimelineController) {
        val tl = TimelineView(timelineState, timelineController, timelineConfig)
        tl.preferredSize = Dimension(800, 0)
        val gbc = GridBagConstraints()
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.VERTICAL
        val items = viewport.view as JPanel
        items.add(tl, gbc)
        items.add(JLabel(), gbc) // Dummy component to force the scrollbars to be left aligned
        items.revalidate()
    }
}
