package presentation.tabpage.timeline

import org.example.state.Arrival
import java.awt.Color
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel

class ArrivalLabel(
    val arrival: Arrival
) : JLabel(arrival.callSign) {

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                background = Color.GRAY // Change color on hover
                isOpaque = true
                repaint()
            }

            override fun mouseExited(e: MouseEvent?) {
                foreground = Color.WHITE // Restore default color
                isOpaque = false
                repaint()
            }
        })
    }

    override fun paintBorder(g: Graphics) {
        super.paintBorder(g)
        g.color = Color.GRAY
        g.drawRoundRect(this.visibleRect.x, visibleRect.y, visibleRect.width - 1, visibleRect.height - 1, 4, 4)
    }
}