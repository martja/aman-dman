package no.vaccsca.amandman.view.tabpage

import no.vaccsca.amandman.controller.ControllerInterface
import no.vaccsca.amandman.view.util.Form
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.event.MouseEvent
import java.time.Instant
import javax.swing.*

class Footer(
    private val controllerInterface: ControllerInterface
) : JPanel(FlowLayout(FlowLayout.RIGHT)) {
    private val timeLabel = JLabel("10:00:22")
    private val metButton = JButton("MET")
    private val profileButton = JButton("Profile")
    private val reloadButton = JButton("Reload settings")
    private val newTabButton = JButton("New tab")
    private val spacingSelector = JSpinner(
        SpinnerNumberModel(3.0, 0.0, 20.0, 1)
    ).apply {
        toolTipText = "Minimum spacing on final"
        addChangeListener {
            val value = value as Double
            controllerInterface.setMinimumSpacingDistance(value)
        }
    }

    init {
        add(spacingSelector)
        add(reloadButton)
        add(metButton)
        add(profileButton)
        add(newTabButton)
        add(JSeparator(SwingConstants.VERTICAL).apply {
            preferredSize = Dimension(2, 20)
        })
        add(timeLabel)

        // Every second, repaint the component
        Timer(1000) {
            repaint()
        }.start()

        metButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                controllerInterface.onOpenMetWindowClicked()
            }
        })

        reloadButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                controllerInterface.onReloadSettingsRequested()
            }
        })

        profileButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                controllerInterface.onOpenVerticalProfileWindowClicked()
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
                        controllerInterface.onNewTimelineGroup(name)
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