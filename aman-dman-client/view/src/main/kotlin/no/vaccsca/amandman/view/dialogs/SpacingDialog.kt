package no.vaccsca.amandman.view.dialogs

import java.awt.Frame
import javax.swing.*

object SpacingDialog {
    fun open(parent: Frame, airportIcao: String, default: Double, onSubmit: (Double) -> Unit) {
        // Spinner for numeric input
        val model = SpinnerNumberModel(default, 0.0, 100.0, 0.1)
        val spinner = JSpinner(model)

        // Panel to hold label + spinner + unit
        val panel = JPanel().apply {
            add(JLabel("Minimum Spacing: "))
            add(spinner)
            add(JLabel("NM"))
        }

        // Show OK / Cancel dialog
        val result = JOptionPane.showConfirmDialog(
            parent,
            panel,
            "Set Minimum Spacing for $airportIcao",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        // If user clicked OK, pass value to callback
        if (result == JOptionPane.OK_OPTION) {
            onSubmit(model.number.toDouble())
        }
    }
}