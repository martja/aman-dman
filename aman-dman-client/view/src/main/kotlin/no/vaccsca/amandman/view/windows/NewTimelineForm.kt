package no.vaccsca.amandman.view.windows

import no.vaccsca.amandman.common.TimelineConfig
import no.vaccsca.amandman.presenter.PresenterInterface
import no.vaccsca.amandman.model.data.dto.CreateOrUpdateTimelineDto
import no.vaccsca.amandman.view.util.Form.enforceUppercase
import java.awt.*
import javax.swing.*

class NewTimelineForm(
    private val presenterInterface: PresenterInterface,
    airportIcao: String,
    existingConfig: TimelineConfig?
) : JPanel() {

    private val titleInput = JTextField(20)
    private val leftRunwaysInput = JTextField(20)
    private val rightRunwaysInput = JTextField(20)

    private val depLayoutCombo = JComboBox<String>()
    private val arrLayoutCombo = JComboBox<String>()

    init {
        border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        layout = GridBagLayout()
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            insets = Insets(5, 5, 5, 5)
        }

        // --- Title input ---
        val titleLabel = JLabel("Timeline Title*")
        enforceUppercase(titleInput)
        gbc.gridx = 0; gbc.gridy = 0
        add(titleLabel, gbc)
        gbc.gridx = 1; gbc.gridy = 0
        add(titleInput, gbc)

        // --- Right runways input ---
        val rightLabel = JLabel("Right side runways* (comma separated)")
        enforceUppercase(rightRunwaysInput)
        gbc.gridx = 0; gbc.gridy = 1
        add(rightLabel, gbc)
        gbc.gridx = 1; gbc.gridy = 1
        add(rightRunwaysInput, gbc)

        // --- Left runways input ---
        val leftLabel = JLabel("Left side runways (comma separated)")
        enforceUppercase(leftRunwaysInput)
        gbc.gridx = 0; gbc.gridy = 2
        add(leftLabel, gbc)
        gbc.gridx = 1; gbc.gridy = 2
        add(leftRunwaysInput, gbc)

        // --- Arrival layout dropdown ---
        val arrLabel = JLabel("Arrival Layout*")
        gbc.gridx = 0; gbc.gridy = 3
        add(arrLabel, gbc)
        gbc.gridx = 1; gbc.gridy = 3
        add(arrLayoutCombo, gbc)

        // --- Departure layout dropdown ---
        val depLabel = JLabel("Departure Layout*")
        gbc.gridx = 0; gbc.gridy = 4
        add(depLabel, gbc)
        gbc.gridx = 1; gbc.gridy = 4
        add(depLayoutCombo, gbc)

        // --- Submit button ---
        val submitButton = JButton("Submit")
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.CENTER
        submitButton.addActionListener { handleSubmit(airportIcao) }
        add(submitButton, gbc)

        // --- Pre-fill existing config ---
        existingConfig?.let { config ->
            titleInput.text = config.title
            leftRunwaysInput.text = config.runwaysLeft.joinToString(",")
            rightRunwaysInput.text = config.runwaysRight.joinToString(",")
            depLayoutCombo.selectedItem = config.depLabelLayout
            arrLayoutCombo.selectedItem = config.arrLabelLayout
        }
    }

    fun update(arrLayouts: Set<String>, depLayouts: Set<String>) {
        depLayoutCombo.removeAllItems()
        depLayouts.forEach { depLayoutCombo.addItem(it) }

        arrLayoutCombo.removeAllItems()
        arrLayouts.forEach { arrLayoutCombo.addItem(it) }

        // Auto select first items if available
        if (depLayoutCombo.itemCount > 0 && depLayoutCombo.selectedItem == null) {
            depLayoutCombo.selectedIndex = 0
        }
        if (arrLayoutCombo.itemCount > 0 && arrLayoutCombo.selectedItem == null) {
            arrLayoutCombo.selectedIndex = 0
        }
    }

    private fun handleSubmit(airportIcao: String) {
        val titleText = titleInput.text.trim()
        val rightText = rightRunwaysInput.text.trim()

        if (titleText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title is required.", "Validation Error", JOptionPane.ERROR_MESSAGE)
            return
        }

        if (rightText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Right side runways are required.", "Validation Error", JOptionPane.ERROR_MESSAGE)
            return
        }

        presenterInterface.onCreateNewTimeline(
            CreateOrUpdateTimelineDto(
                airportIcao = airportIcao,
                title = titleText,
                left = CreateOrUpdateTimelineDto.TimeLineSide(
                    targetRunways = leftRunwaysInput.text.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                ),
                right = CreateOrUpdateTimelineDto.TimeLineSide(
                    targetRunways = rightRunwaysInput.text.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                ),
                depLabelLayout = depLayoutCombo.selectedItem as? String ?: "",
                arrLabelLayout = arrLayoutCombo.selectedItem as? String ?: ""
            )
        )
    }
}
