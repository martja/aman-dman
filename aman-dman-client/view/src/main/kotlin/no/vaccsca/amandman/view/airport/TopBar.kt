package no.vaccsca.amandman.view.airport

import no.vaccsca.amandman.presenter.PresenterInterface
import java.awt.*
import javax.swing.*

class TopBar(
    private val presenter: PresenterInterface,
    private val airportIcao: String,
) : JPanel() {
    private val showDepartures = JCheckBox("Departures")
    private val nonSequencedButton = JButton("NonSeq")
    private val landingRatesButton = JButton("Landing Rates")
    private val runwayModeList = JPanel(FlowLayout(FlowLayout.LEFT, 10, 5))

    init {
        layout = BorderLayout()

        showDepartures.addActionListener {
            presenter.onToggleShowDepartures(airportIcao, showDepartures.isSelected)
        }

        landingRatesButton.addActionListener {
            presenter.onOpenLandingRatesWindow()
        }

        nonSequencedButton.addActionListener {
            presenter.onOpenNonSequencedWindow()
        }

        // Right-aligned controls
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 5))
        rightPanel.add(showDepartures)
        rightPanel.add(nonSequencedButton)
        rightPanel.add(landingRatesButton)

        // Wrap runwayModeList in a container to center it vertically
        val runwayModeContainer = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.anchor = GridBagConstraints.WEST
        runwayModeContainer.add(runwayModeList, gbc)

        add(runwayModeContainer, BorderLayout.WEST)
        add(rightPanel, BorderLayout.EAST)
    }

    fun updateNonSeqNumbers(numberOfNonSeq: Int) {
        this.nonSequencedButton.apply {
            background = if (numberOfNonSeq > 0) Color.YELLOW else Color.GRAY
            text = "NonSeq ($numberOfNonSeq)"
            foreground = if (numberOfNonSeq > 0) Color.BLACK else Color.WHITE
        }
    }

    fun setRunwayModes(runwayModes: List<Pair<String, Boolean>>) {
        runwayModeList.removeAll()
        runwayModes.forEach { (modeName, isActive) ->
            val label = JLabel(modeName)
            label.foreground = if (isActive) Color.WHITE else Color.GRAY
            runwayModeList.add(label)
        }
        runwayModeList.revalidate()
        runwayModeList.repaint()
    }
}
