package no.vaccsca.amandman.view.coponents

import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton

class ReloadButton(
    tooltip: String,
    action: () -> Unit,
): JButton("⟳") {
    init {
        font = Font(font.name, Font.BOLD, 20)
        toolTipText = tooltip
        margin = Insets(0, 0, 5, 0)
        preferredSize = Dimension(22, 22)
        border = BorderFactory.createEmptyBorder(1,1,6,1)
        addActionListener {
            action()
        }
    }
}