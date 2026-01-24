package no.vaccsca.amandman.view.components

import javax.swing.DefaultDesktopManager
import javax.swing.JComponent
import javax.swing.JInternalFrame
import kotlin.math.max
import kotlin.math.min

class BoundedDesktopManager : DefaultDesktopManager() {
    override fun beginDraggingFrame(f: JComponent?) {
        // Don't do anything. Needed to prevent the DefaultDesktopManager setting the dragMode
    }

    override fun beginResizingFrame(f: JComponent?, direction: Int) {
        // Don't do anything. Needed to prevent the DefaultDesktopManager setting the dragMode
    }

    override fun setBoundsForFrame(f: JComponent, newX: Int, newY: Int, newWidth: Int, newHeight: Int) {
        val didResize = (f.getWidth() != newWidth || f.getHeight() != newHeight)
        if (!inBounds(f as JInternalFrame, newX, newY, newWidth, newHeight)) {
            val parent = f.getParent()
            val parentSize = parent.getSize()
            val boundedX = min(max(0, newX).toDouble(), parentSize.getWidth() - newWidth).toInt()
            val boundedY = min(max(0, newY).toDouble(), parentSize.getHeight() - newHeight).toInt()
            f.setBounds(boundedX, boundedY, newWidth, newHeight)
        } else {
            f.setBounds(newX, newY, newWidth, newHeight)
        }
        if (didResize) {
            f.validate()
        }
    }

    protected fun inBounds(f: JInternalFrame, newX: Int, newY: Int, newWidth: Int, newHeight: Int): Boolean {
        if (newX < 0 || newY < 0) return false
        if (newX + newWidth > f.getDesktopPane().getWidth()) return false
        if (newY + newHeight > f.getDesktopPane().getHeight()) return false
        return true
    }
}
