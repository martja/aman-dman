package org.example.presentation.tabpage

import java.awt.FlowLayout
import java.awt.Graphics
import java.time.Instant
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.Timer

class Footer : JPanel(FlowLayout(FlowLayout.RIGHT)) {
    private val timeLabel = JLabel("10:00:22")

    init {
        add(timeLabel)

        // Every second, repaint the component
        Timer(1000) {
            repaint()
        }.start()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        timeLabel.text = Instant.now().toString().substring(11, 19)
    }
}