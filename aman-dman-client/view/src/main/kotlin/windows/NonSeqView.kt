package windows

import org.example.RunwayArrivalOccurrence
import org.example.SequenceStatus
import org.example.TimelineOccurrence
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.table.DefaultTableModel

class NonSeqView : JPanel() {
    private val columnNames = arrayOf("No.", "Call Sign", "A/C", "WVC")
    private val tableModel = object : DefaultTableModel(columnNames, 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JTable(tableModel)

    init {
        layout = BorderLayout()

        // Table setup
        table.autoCreateRowSorter = true
        table.tableHeader.reorderingAllowed = true
        table.tableHeader.resizingAllowed = true

        val scrollPane = JScrollPane(table)

        val groupPanel = JPanel(BorderLayout()).apply {
            border = TitledBorder("Arrivals")
            add(scrollPane, BorderLayout.CENTER)
        }

        add(groupPanel, BorderLayout.CENTER)
    }

    fun updateNonSeqData(data: List<TimelineOccurrence>) {
        val nonSequenced = data
            .filterIsInstance<RunwayArrivalOccurrence>()
            .filter { it.sequenceStatus == SequenceStatus.NEEDS_MANUAL_INSERTION }

        tableModel.setRowCount(0) // Efficiently clear existing rows

        nonSequenced.forEachIndexed { index, it ->
            tableModel.addRow(arrayOf(
                index + 1,
                it.callsign,
                it.icaoType,
                it.wakeCategory
            ))
        }
    }
}
