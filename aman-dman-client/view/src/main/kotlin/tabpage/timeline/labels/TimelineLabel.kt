package tabpage.timeline.labels

import kotlinx.datetime.Instant
import org.example.TimelineOccurrence
import java.awt.Color
import javax.swing.JLabel

abstract class TimelineLabel(
    var timelineOccurrence: TimelineOccurrence,
    val defaultBackgroundColor: Color? = null,
    val defaultForegroundColor: Color = Color.WHITE,
    val hoverBackgroundColor: Color? = Color.GRAY,
    val hoverForegroundColor: Color = Color.BLACK
) : JLabel() {
    init {
        background = defaultBackgroundColor
        foreground = defaultForegroundColor
        isOpaque = defaultBackgroundColor != null

        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseEntered(e: java.awt.event.MouseEvent?) {
                background = hoverBackgroundColor // Change color on hover
                foreground = hoverForegroundColor
                isOpaque = hoverBackgroundColor != null
                repaint()
            }

            override fun mouseExited(e: java.awt.event.MouseEvent?) {
                background = defaultBackgroundColor // Restore default color
                foreground = defaultForegroundColor
                isOpaque = defaultBackgroundColor != null
                repaint()
            }
        })
    }

    abstract fun updateText()

    abstract fun getTimelinePlacement(): Instant

    protected open fun getBorderColor(): Color {
        return Color.GRAY
    }

    override fun paintBorder(g: java.awt.Graphics) {
        super.paintBorder(g)
        g.color = getBorderColor()
        g.drawRoundRect(this.visibleRect.x, visibleRect.y, visibleRect.width - 1, visibleRect.height - 1, 4, 4)
    }
}