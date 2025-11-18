package no.vaccsca.amandman.view.airport

import no.vaccsca.amandman.common.NtpClock
import no.vaccsca.amandman.model.UserRole
import no.vaccsca.amandman.model.data.repository.SettingsRepository
import no.vaccsca.amandman.presenter.PresenterInterface
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.MouseEvent
import javax.swing.*

class Footer(
    private val presenterInterface: PresenterInterface,
    private val mainWindow: JFrame
) : JPanel(FlowLayout(FlowLayout.RIGHT)) {
    private val timeLabel = JLabel("10:00:22")
    private val metButton = JButton("MET")
    private val profileButton = JButton("Profile")
    private val reloadButton = JButton("Reload settings")
    private val newTabButton = JButton("New group")

    init {
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
                val availableAirportIcaos = SettingsRepository.getAirportData().map { it.icao }
                val icaoComboBox = JComboBox(availableAirportIcaos.toTypedArray())

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
                    mainWindow,
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
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        timeLabel.text = NtpClock.now().toString().substring(11, 19)
    }
}