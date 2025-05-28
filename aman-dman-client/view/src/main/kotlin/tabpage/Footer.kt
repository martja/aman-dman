package tabpage

import ControllerInterface
import util.Form
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.event.MouseEvent
import java.time.Instant
import javax.swing.*

class Footer(controllerInterface: ControllerInterface?) : JPanel(FlowLayout(FlowLayout.RIGHT)) {
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
                controllerInterface?.onOpenMetWindowClicked()
            }
        })

        loadAllButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                controllerInterface?.onLoadAllTabsRequested()
            }
        })

        profileButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                controllerInterface?.onOpenVerticalProfileWindowClicked()
            }
        })

        newTabButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                val textField = JTextField()
                Form.enforceUppercase(textField, 4)

                val result = JOptionPane.showConfirmDialog(
                    null,
                    textField,
                    "Airport ICAO",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
                )

                if (result == JOptionPane.OK_OPTION) {
                    val name = textField.text
                    if (name.isNotBlank()) {
                        controllerInterface?.onNewTimelineGroup(name)
                    }
                }
            }
        })
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        timeLabel.text = Instant.now().toString().substring(11, 19)
    }
}