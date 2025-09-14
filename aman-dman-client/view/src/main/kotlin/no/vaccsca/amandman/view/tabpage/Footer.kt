package no.vaccsca.amandman.view.tabpage

import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.view.util.Form
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.event.MouseEvent
import java.time.Instant
import javax.swing.*

class Footer(
    private val presenterInterface: PresenterInterface
) : JPanel(FlowLayout(FlowLayout.RIGHT)) {
    private val timeLabel = JLabel("10:00:22")
    private val metButton = JButton("MET")
    private val profileButton = JButton("Profile")
    private val reloadButton = JButton("Reload settings")
    private val newTabButton = JButton("New tab")
    private val spacingSelector = JSpinner(SpinnerNumberModel(0.0, 0.0, 20.0, 1))
    private var spacingChangeListener: javax.swing.event.ChangeListener? = null

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
                presenterInterface.onOpenMetWindowClicked()
            }
        })

        reloadButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                presenterInterface.onReloadSettingsRequested()
            }
        })

        profileButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                presenterInterface.onOpenVerticalProfileWindowClicked()
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
                        presenterInterface.onNewTimelineGroup(name)
                    }
                }
            }
        })

        spacingSelector.apply {
            toolTipText = "Minimum spacing on final"
            (editor as JSpinner.NumberEditor).textField.apply {
                isEditable = false
                isFocusable = false
            }
            spacingChangeListener = javax.swing.event.ChangeListener {
                presenterInterface.setMinimumSpacingDistance("ENGM", value as Double)
            }
            addChangeListener(spacingChangeListener)
        }
    }

    fun updateMinimumSpacingSelector(value: Double) {
        // Temporarily remove listener to avoid feedback loop
        spacingChangeListener?.let { spacingSelector.removeChangeListener(it) }
        spacingSelector.value = value
        spacingChangeListener?.let { spacingSelector.addChangeListener(it) }
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        timeLabel.text = Instant.now().toString().substring(11, 19)
    }
}