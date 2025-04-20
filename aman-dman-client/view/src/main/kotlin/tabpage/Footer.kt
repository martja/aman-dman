package tabpage

import org.example.eventHandling.ViewListener
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.event.MouseEvent
import java.time.Instant
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.Timer

class Footer(viewListener: ViewListener?) : JPanel(FlowLayout(FlowLayout.RIGHT)) {
    private val timeLabel = JLabel("10:00:22")
    private val metButton = JButton("MET")
    private val profileButton = JButton("Profile")
    private val loadAllButton = JButton("Load all")
    private val newTabButton = JButton("New tab")

    init {
        add(metButton)
        add(profileButton)
        add(timeLabel)
        add(loadAllButton)
        add(newTabButton)

        // Every second, repaint the component
        Timer(1000) {
            repaint()
        }.start()

        metButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                viewListener?.onOpenMetWindowClicked()
            }
        })

        loadAllButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                viewListener?.onLoadAllTabsRequested()
            }
        })

        profileButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                viewListener?.onOpenVerticalProfileWindowClicked()
            }
        })

        newTabButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                val name = JOptionPane.showInputDialog(this, "Enter timeline name")
                if (!name.isNullOrBlank()) {
                    viewListener?.onNewTimelineGroup(name)
                }
            }
        })
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        timeLabel.text = Instant.now().toString().substring(11, 19)
    }
}