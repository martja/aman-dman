package no.vaccsca.amandman.view.components

import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout

/**
 * A FlowLayout subclass that properly calculates preferred/minimum sizes
 * so components wrap based on the container's current width.
 *
 * Implementation inspired by common WrapLayout patterns (keeps lightweight, Kotlin-ified).
 */
class WrapLayout(hAlign: Int = LEFT, private val hGap: Int = 5, private val vGap: Int = 5) :
    FlowLayout(hAlign, hGap, vGap) {

    override fun preferredLayoutSize(target: Container): Dimension {
        return layoutSize(target, true)
    }

    override fun minimumLayoutSize(target: Container): Dimension {
        return layoutSize(target, false)
    }

    private fun layoutSize(target: Container, preferred: Boolean): Dimension {
        synchronized(target.treeLock) {
            val insets = target.insets
            val maxWidth = (target.width.takeIf { it > 0 } ?: Int.MAX_VALUE) - insets.left - insets.right - hGap * 2
            var width = 0
            var height = insets.top + insets.bottom
            var rowWidth = 0
            var rowHeight = 0
            val nmembers = target.componentCount

            for (i in 0 until nmembers) {
                val m = target.getComponent(i)
                if (!m.isVisible) continue
                val d = if (preferred) m.preferredSize else m.minimumSize
                if (rowWidth + d.width + hGap > maxWidth && rowWidth > 0) {
                    // new row
                    width = maxOf(width, rowWidth)
                    height += rowHeight + vGap
                    rowWidth = 0
                    rowHeight = 0
                }
                rowWidth += d.width + hGap
                rowHeight = maxOf(rowHeight, d.height)
            }

            // last row
            width = maxOf(width, rowWidth)
            height += rowHeight

            // include insets and some extra gaps
            width += insets.left + insets.right + hGap * 2
            height += vGap

            return Dimension(width, height)
        }
    }
}