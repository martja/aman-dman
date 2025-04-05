package org.example.presentation.tabpage

import org.example.controller.MainController
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.event.MouseEvent
import java.time.Instant
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.Timer

class Footer(mainController: MainController) : JPanel(FlowLayout(FlowLayout.RIGHT)) {
    private val timeLabel = JLabel("10:00:22")
    private val metButton = JButton("MET")

    init {
        add(metButton)
        add(timeLabel)

        // Every second, repaint the component
        Timer(1000) {
            repaint()
        }.start()

        metButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                mainController.openMetWindow()
                mainController.openProfileWindow()

            }
        })
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        timeLabel.text = Instant.now().toString().substring(11, 19)
    }
}