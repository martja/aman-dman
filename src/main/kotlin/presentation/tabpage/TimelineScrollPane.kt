package org.example.presentation.tabpage

import org.example.presentation.tabpage.timeline.TimelineView
import org.example.state.TimelineState
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane


class TimelineScrollPane(timelineState: TimelineState) : JScrollPane(VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
    init {
        val gbc = GridBagConstraints()
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.VERTICAL

        val items = JPanel()
        items.layout = GridBagLayout()

        for (index in 0..0) {
            val tl = TimelineView(timelineState)
            tl.preferredSize = Dimension(500, 0)
            items.add(tl, gbc)
        }

        gbc.weightx = 1.0
        items.add(JLabel(), gbc) // Dummy component to force the scrollbars to be left aligned
        viewport.add(items)
    }
}
