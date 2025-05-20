package newTimelineWindow

import org.example.TimelineConfig
import org.example.dto.CreateOrUpdateTimelineDto
import org.example.eventHandling.ViewListener
import java.awt.*
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

class NewTimelineForm(val viewListener: ViewListener, groupId: String, existingConfig: TimelineConfig?) : JPanel() {
    private val icaoField = JTextField(4)

    private val radioButtonRunway = JRadioButton("Runway")
    private val radioButtonAcc = JRadioButton("ACC")

    private val modeGroup: ButtonGroup = ButtonGroup().apply {
        add(radioButtonRunway)
        add(radioButtonAcc)
    }

    // Store references to input fields and checkboxes for data access
    private lateinit var leftFixesInput: JTextField
    private lateinit var leftRunwaysInput: JTextField
    private lateinit var leftEnabledCheckbox: JCheckBox

    private lateinit var rightFixesInput: JTextField
    private lateinit var rightRunwaysInput: JTextField
    private lateinit var rightEnabledCheckbox: JCheckBox

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(formRow("Airport ICAO:", icaoField))

        add(formRow("Mode:", JPanel().apply {
            add(radioButtonRunway)
            add(radioButtonAcc)
        }))

        val fixListsPanel = JPanel(GridLayout(1, 2, 10, 10))
        fixListsPanel.add(createFixPanel("Left", true))
        fixListsPanel.add(createFixPanel("Right", false))

        add(fixListsPanel)

        existingConfig?.let { config ->
            icaoField.text = config.airportIcao
            leftFixesInput.text = config.targetFixesLeft.joinToString(", ")
            leftRunwaysInput.text = config.runwayLeft
            rightFixesInput.text = config.targetFixesRight.joinToString(", ")
            rightRunwaysInput.text = config.runwayRight
        }

        val submitButton = JButton("Submit")
        submitButton.alignmentX = CENTER_ALIGNMENT
        submitButton.addActionListener {
            viewListener.onCreateNewTimeline(
                CreateOrUpdateTimelineDto(
                    groupId = groupId,
                    title = "${icaoField.text} Timeline",
                    airportIcao = icaoField.text.trim().uppercase(),
                    left = CreateOrUpdateTimelineDto.TimeLineSide(
                        targetFixes = leftFixesInput.text.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() },
                        targetRunways = leftRunwaysInput.text.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                    ),
                    right = CreateOrUpdateTimelineDto.TimeLineSide(
                        targetFixes = rightFixesInput.text.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() },
                        targetRunways = rightRunwaysInput.text.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                    )
                )
            )
        }

        add(Box.createVerticalStrut(10))
        add(submitButton)
    }

    private fun createFixPanel(title: String, isLeft: Boolean): JPanel {
        val panel = JPanel(BorderLayout(5, 5))
        panel.border = BorderFactory.createTitledBorder(title)

        val enabledCheckBox = JCheckBox("Enabled")
        panel.add(enabledCheckBox, BorderLayout.NORTH)

        val inputPanel = JPanel()
        inputPanel.layout = BoxLayout(inputPanel, BoxLayout.Y_AXIS)

        fun createLabeledField(labelText: String): JTextField {
            val container = JPanel()
            container.layout = BoxLayout(container, BoxLayout.Y_AXIS)

            val label = JLabel(labelText)
            val textField = JTextField()
            enforceUppercase(textField)

            textField.maximumSize = Dimension(Int.MAX_VALUE, textField.preferredSize.height)
            textField.alignmentX = Component.LEFT_ALIGNMENT

            container.add(label)
            container.add(Box.createVerticalStrut(2))
            container.add(textField)
            container.alignmentX = Component.LEFT_ALIGNMENT

            inputPanel.add(container)
            inputPanel.add(Box.createVerticalStrut(5))

            return textField
        }

        val idInput = createLabeledField("Timeline label*:")
        val fixesInput = createLabeledField("Target fixes* (comma separated):")
        val runwayInput = createLabeledField("Assigned runways (comma separated):")

        // Store references
        if (isLeft) {
            leftEnabledCheckbox = enabledCheckBox
            leftFixesInput = fixesInput
            leftRunwaysInput = runwayInput
        } else {
            rightEnabledCheckbox = enabledCheckBox
            rightFixesInput = fixesInput
            rightRunwaysInput = runwayInput
        }

        panel.add(inputPanel, BorderLayout.CENTER)
        return panel
    }

    private fun formRow(label: String, component: JComponent): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.add(JLabel(label))
        panel.add(component)
        return panel
    }

    fun enforceUppercase(textField: JTextField) {
        val doc = textField.document
        if (doc is AbstractDocument) {
            doc.documentFilter = object : DocumentFilter() {
                override fun insertString(fb: FilterBypass, offset: Int, string: String?, attr: AttributeSet?) {
                    if (string != null) {
                        super.insertString(fb, offset, string.uppercase(), attr)
                    }
                }

                override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: AttributeSet?) {
                    if (text != null) {
                        super.replace(fb, offset, length, text.uppercase(), attrs)
                    }
                }
            }
        }
    }
}
