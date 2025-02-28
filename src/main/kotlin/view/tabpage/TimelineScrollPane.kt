package org.example.presentation.tabpage

import model.entities.TimelineConfig
import org.example.presentation.tabpage.timeline.TimelineView
import org.example.state.ApplicationState
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane


class TimelineScrollPane(applicationState: ApplicationState) : JScrollPane(VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
    init {
        val gbc = GridBagConstraints()
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.VERTICAL

        val items = JPanel()
        items.layout = GridBagLayout()

        for (index in 0..0) {
            val tl = TimelineView(applicationState, TimelineConfig("GM 01L/01R", listOf( "OBW40", "ONE40"), listOf( "VALPU", "INSUV")))
            tl.preferredSize = Dimension(800, 0)

            val tl2 = TimelineView(applicationState, TimelineConfig("VALPU INSUV", listOf( "VALPU", "INSUV"), listOf( "VALPU", "INSUV")))
            tl2.preferredSize = Dimension(800, 0)

            items.add(tl, gbc)
            items.add(tl2, gbc)
        }

        gbc.weightx = 1.0
        items.add(JLabel(), gbc) // Dummy component to force the scrollbars to be left aligned
        viewport.add(items)
    }
}
