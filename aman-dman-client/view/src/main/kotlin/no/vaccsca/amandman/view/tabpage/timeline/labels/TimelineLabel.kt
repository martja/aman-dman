package no.vaccsca.amandman.view.tabpage.timeline.labels

import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.domain.valueobjects.timelineEvent.TimelineEvent
import java.awt.Color
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.border.EmptyBorder

abstract class TimelineLabel(
    var timelineEvent: TimelineEvent,
    val defaultBackgroundColor: Color? = null,
    var defaultForegroundColor: Color = Color.WHITE,
    val hoverBackgroundColor: Color? = Color.GRAY,
    val hoverForegroundColor: Color = Color.BLACK,
    hBorder: Int,
    vBorder: Int,
) : JLabel() {
    private var isHovered: Boolean = false
    private var isDragging: Boolean = false

    init {
        background = defaultBackgroundColor
        foreground = defaultForegroundColor
        isOpaque = defaultBackgroundColor != null
        border = EmptyBorder(vBorder, hBorder, vBorder, hBorder)

        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseEntered(e: java.awt.event.MouseEvent?) {
                isHovered = true
                updateColors()
            }

            override fun mouseExited(e: java.awt.event.MouseEvent?) {
                isHovered = false
                updateColors()
            }
        })
    }

    fun updateColors() {
        if (isHovered || isDragging) {
            background = hoverBackgroundColor
            foreground = hoverForegroundColor
            isOpaque = hoverBackgroundColor != null
        } else {
            background = defaultBackgroundColor
            foreground = defaultForegroundColor
            isOpaque = defaultBackgroundColor != null
        }
        repaint()
    }

    fun onDragStart() {
        isDragging = true
        updateColors()
    }

    fun onDragEnd() {
        isDragging = false
        updateColors()
    }

    abstract fun updateText()

    abstract fun getTimelinePlacement(): Instant

    protected open fun getBorderColor(): Color {
        return Color.GRAY
    }

    override fun paintBorder(g: java.awt.Graphics) {
        super.paintBorder(g)
        //g.color = getBorderColor()
        //g.drawRoundRect(this.visibleRect.x, visibleRect.y, visibleRect.width - 1, visibleRect.height - 1, 4, 4)
    }
}