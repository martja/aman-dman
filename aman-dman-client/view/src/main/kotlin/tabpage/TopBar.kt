package tabpage

import ControllerInterface
import java.awt.*
import java.awt.event.*
import javax.swing.*

class TopBar(
    private val controller: ControllerInterface
) : JPanel() {

    init {
        layout = BorderLayout()

        // Left-aligned labels
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 5))
        val toggles = listOf("S01R", "01L:3.0", "01R:3.0","19L:3.0", "19R:3.0", "01L/01R:3.0", "19L/19R:3.0")
        toggles.forEach { labelText ->
            leftPanel.add(ClickableLabel(labelText))
        }

        val landingRatesButton = JButton("Landing Rates")
        landingRatesButton.addActionListener {
            controller.onOpenLandingRatesWindow()
        }

        // Right-aligned controls
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 5))
        rightPanel.add(landingRatesButton)

        add(leftPanel, BorderLayout.WEST)
        add(rightPanel, BorderLayout.EAST)
    }

    private class ClickableLabel(text: String) : JLabel(text) {
        private var active = false

        init {
            foreground = Color.GRAY
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            isOpaque = false

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    active = !active
                    foreground = if (active) Color.WHITE else Color.GRAY
                }

                override fun mouseEntered(e: MouseEvent?) {
                    foreground = if (active) Color.WHITE else Color.LIGHT_GRAY
                }

                override fun mouseExited(e: MouseEvent?) {
                    foreground = if (active) Color.WHITE else Color.GRAY
                }
            })
        }
    }
}
