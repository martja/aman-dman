package newTimelineWindow

import org.example.TimelineConfig
import org.example.dto.CreateOrUpdateTimelineDto
import org.example.eventHandling.ViewListener
import java.awt.*
import javax.swing.*

class NewTimelineForm(val viewListener: ViewListener, groupId: String, existingConfig: TimelineConfig?) : JPanel() {
    private val titleField = JTextField(20)
    private val icaoField = JTextField(10)

    private val leftFixListModel = DefaultListModel<String>()
    private val rightFixListModel = DefaultListModel<String>()

    private val leftFixList = JList(leftFixListModel)
    private val rightFixList = JList(rightFixListModel)

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        add(formRow("Title:", titleField))
        add(formRow("Airport ICAO:", icaoField))

        val fixListsPanel = JPanel(GridLayout(1, 2, 10, 10))
        fixListsPanel.add(createFixPanel("Left target fixes", leftFixListModel, leftFixList))
        fixListsPanel.add(createFixPanel("Right target fixes", rightFixListModel, rightFixList))
        add(fixListsPanel)

        // 🟡 Apply existing config values here
        existingConfig?.let { config ->
            titleField.text = config.title
            icaoField.text = config.airportIcao
            config.targetFixesLeft.forEach { leftFixListModel.addElement(it) }
            config.targetFixesRight.forEach { rightFixListModel.addElement(it) }
        }

        val submitButton = JButton("Submit")
        submitButton.alignmentX = CENTER_ALIGNMENT
        submitButton.addActionListener {
            viewListener.onCreateNewTimeline(CreateOrUpdateTimelineDto(
                groupId = groupId,
                title = titleField.text,
                airportIcao = icaoField.text,
                targetFixesLeft = (0 until leftFixListModel.size()).map { leftFixListModel.getElementAt(it) },
                targetFixesRight = (0 until rightFixListModel.size()).map { rightFixListModel.getElementAt(it) }
            ))
        }
        add(Box.createVerticalStrut(10))
        add(submitButton)
    }

    private fun createFixPanel(title: String, model: DefaultListModel<String>, list: JList<String>): JPanel {
        val panel = JPanel(BorderLayout(5, 5))
        panel.border = BorderFactory.createTitledBorder(title)

        list.visibleRowCount = 6
        panel.add(JScrollPane(list), BorderLayout.CENTER)

        val buttonPanel = JPanel(GridLayout(1, 2, 5, 5))
        val addButton = JButton("Add")
        val removeButton = JButton("Remove")
        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)

        panel.add(buttonPanel, BorderLayout.SOUTH)

        addButton.addActionListener {
            val fix = JOptionPane.showInputDialog(this, "Enter fix name for $title:")
            if (!fix.isNullOrBlank()) model.addElement(fix)
        }

        removeButton.addActionListener {
            val selected = list.selectedIndex
            if (selected >= 0) model.remove(selected)
        }

        return panel
    }

    private fun formRow(label: String, component: JComponent): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.add(JLabel(label))
        panel.add(component)
        return panel
    }
}