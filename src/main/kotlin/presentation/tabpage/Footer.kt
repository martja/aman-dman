package org.example.presentation.tabpage

import java.awt.FlowLayout
import java.awt.Graphics
import javax.swing.JLabel
import javax.swing.JPanel

class Footer : JPanel() {
    private val timeLabel = JLabel("10:00:22")

    init {
        layout = FlowLayout()



        add(timeLabel)
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)



    }
}