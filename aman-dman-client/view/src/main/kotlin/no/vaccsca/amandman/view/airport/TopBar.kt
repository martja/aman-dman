package no.vaccsca.amandman.view.airport

import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.view.components.WrapLayout
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

class TopBar(
    private val presenter: PresenterInterface,
    private val airportIcao: String,
) : JPanel() {
    private val showDepartures = JCheckBox("Departures")
    private val nonSequencedButton = JButton("NonSeq")
    private val landingRatesButton = JButton("Landing Rates")
    // Use WrapLayout so the labels wrap based on available width
    private val runwayModeList = JPanel(WrapLayout(FlowLayout.LEFT, 10, 5))

    init {
        layout = BorderLayout()

        showDepartures.addActionListener {
            presenter.onToggleShowDepartures(airportIcao, showDepartures.isSelected)
        }

        landingRatesButton.addActionListener {
            presenter.onOpenLandingRatesWindow(airportIcao)
        }

        nonSequencedButton.addActionListener {
            presenter.onOpenNonSequencedWindow(airportIcao)
        }

        // Right-aligned controls
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 5))
        rightPanel.add(showDepartures)
        rightPanel.add(nonSequencedButton)
        rightPanel.add(landingRatesButton)

        // Place runwayModeList in CENTER so it can expand vertically/horizontally
        add(runwayModeList, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

        // Revalidate/repaint the runwayModeList on resize to force WrapLayout recalculation
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                runwayModeList.revalidate()
                runwayModeList.repaint()
            }
        })
    }

    fun updateNonSeqNumbers(numberOfNonSeq: Int) {
        this.nonSequencedButton.apply {
            background = if (numberOfNonSeq > 0) Color.YELLOW else null
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
