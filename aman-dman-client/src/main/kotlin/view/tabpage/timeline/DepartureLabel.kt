package org.example.view.tabpage.timeline

import org.example.state.Departure
import javax.swing.JLabel

class DepartureLabel(var departure: Departure) : JLabel() {
    init {
        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseEntered(e: java.awt.event.MouseEvent?) {
                background = java.awt.Color.GRAY // Change color on hover
                isOpaque = true
                repaint()
            }

            override fun mouseExited(e: java.awt.event.MouseEvent?) {
                foreground = java.awt.Color.WHITE // Restore default color
                isOpaque = false
                repaint()
            }
        })

        updateText()
    }

    override fun paintBorder(g: java.awt.Graphics) {
        super.paintBorder(g)
        g.color = java.awt.Color.GRAY
        g.drawRoundRect(this.visibleRect.x, visibleRect.y, visibleRect.width - 1, visibleRect.height - 1, 4, 4)
    }

    fun updateText() {
        var output = "<html><pre>"

        output += departure.callsign.padEnd(4)
        output += departure.runway?.padEnd(8)
        output += departure.sid?.padEnd(9)
        output += departure.icaoType?.padEnd(5)
        output += departure.wakeCategory.toString().padEnd(2)

        output += "</pre></html>"

        text = output
    }
}