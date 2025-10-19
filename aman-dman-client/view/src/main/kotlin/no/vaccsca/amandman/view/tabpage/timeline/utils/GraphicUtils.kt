package no.vaccsca.amandman.view.tabpage.timeline.utils

import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle

object GraphicUtils {
    fun Graphics.drawCenteredString(text: String, x: Int, y: Int, backgroundColor: Color? = null) {
        val metrics = this.getFontMetrics(font)

        val textWidth = fontMetrics.stringWidth(text)
        val textBounds = Rectangle(
            x - textWidth / 2,
            y - fontMetrics.height / 2,
            fontMetrics.stringWidth(text),
            fontMetrics.height
        )

        if (backgroundColor != null) {
            val originalColor = this.color
            this.color = backgroundColor
            textBounds.grow(4, 0)
            this.fillRect(
                textBounds.x, textBounds.y, textBounds.width, textBounds.height
            )
            this.color = originalColor
        }

        val x = textBounds.x + (textBounds.width - metrics.stringWidth(text)) / 2
        val y = textBounds.y + ((textBounds.height - metrics.height) / 2) + metrics.ascent
        this.font = font
        this.drawString(text, x, y)
    }
}