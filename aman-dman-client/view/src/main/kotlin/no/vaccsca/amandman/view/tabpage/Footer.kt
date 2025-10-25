package no.vaccsca.amandman.view.tabpage

import no.vaccsca.amandman.model.UserRole
import no.vaccsca.amandman.model.data.repository.SettingsRepository
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.view.util.Form
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
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
    private val spacingSelector = JSpinner(SpinnerNumberModel(3.0, 0.0, 20.0, 1))
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

                // Get available ICAOs and sort them alphabetically
                val availableAirports = SettingsRepository.getAirportData().airports.keys.sorted()
                val icaoComboBox = JComboBox(availableAirports.toTypedArray())

                val roles = UserRole.entries.toTypedArray()
                val roleComboBox = JComboBox(roles)

                val panel = JPanel(GridBagLayout()).apply {
                    val gbc = GridBagConstraints().apply {
                        fill = GridBagConstraints.HORIZONTAL
                        insets = Insets(5, 5, 5, 5)
                        anchor = GridBagConstraints.WEST
                    }

                    // --- ICAO ComboBox ---
                    gbc.gridx = 0
                    gbc.gridy = 0
                    add(JLabel("Airport"), gbc)

                    gbc.gridx = 1
                    gbc.gridy = 0
                    add(icaoComboBox, gbc)

                    // --- Role ComboBox ---
                    gbc.gridx = 0
                    gbc.gridy = 1
                    add(JLabel("User Role"), gbc)

                    gbc.gridx = 1
                    gbc.gridy = 1
                    add(roleComboBox, gbc)
                }

                val result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "New Timeline Group",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
                )

                if (result == JOptionPane.OK_OPTION) {
                    val icao = icaoComboBox.selectedItem as? String
                    val role = roleComboBox.selectedItem as? UserRole
                    if (!icao.isNullOrBlank() && role != null) {
                        presenterInterface.onNewTimelineGroup(icao, role)
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
                presenterInterface.onMinimumSpacingDistanceSet("ENGM", value as Double)
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