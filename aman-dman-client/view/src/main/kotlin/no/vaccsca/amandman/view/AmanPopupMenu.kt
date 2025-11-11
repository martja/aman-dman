package no.vaccsca.amandman.view

import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSeparator
import javax.swing.SwingConstants

class AmanPopupMenu(title: String, vararg itemGroups: AmanMenuItemData) : JPopupMenu() {
    init {
        add(JLabel("<html><div style='text-align: center;'>$title</div></html>", SwingConstants.CENTER))
        add(JSeparator())

        itemGroups.forEach { itemData ->
            addMenuItemRecursively(itemData, this)
        }
    }

    private fun addMenuItemRecursively(itemData: AmanMenuItemData, parent: JComponent) {
        if (itemData.children.isNotEmpty()) {
            // Create a submenu for children
            val submenu = JMenu(itemData.title)
            itemData.children.forEach { child ->
                addMenuItemRecursively(child, submenu)
            }
            parent.add(submenu)
        } else {
            // Create a leaf menu item
            val item = JMenuItem(itemData.title)
            item.addActionListener { itemData.action?.invoke() }
            parent.add(item)
        }

        if (itemData.separatorAfter) {
            parent.add(JSeparator())
        }
    }
}

data class AmanMenuItemData(
    val title: String,
    val children: List<AmanMenuItemData> = emptyList(),
    val separatorAfter: Boolean = false,
    val action: (() -> Unit)? = null,
)

// --- DSL Builders ---

class AmanPopupMenuBuilder {
    internal val items = mutableListOf<AmanMenuItemData>()

    fun item(
        title: String,
        separatorAfter: Boolean = false,
        action: (() -> Unit)? = null,
        block: (AmanPopupMenuBuilder.() -> Unit)? = null
    ) {
        val children = if (block != null) {
            AmanPopupMenuBuilder().apply(block).items
        } else {
            emptyList()
        }

        items += AmanMenuItemData(
            title = title,
            children = children,
            separatorAfter = separatorAfter,
            action = action
        )
    }

    fun separator() {
        if (items.isNotEmpty()) {
            val lastItem = items.removeAt(items.size - 1)
            items += lastItem.copy(separatorAfter = true)
        }
    }
}

// --- Entry function ---

fun AmanPopupMenu(
    title: String,
    block: AmanPopupMenuBuilder.() -> Unit
): AmanPopupMenu {
    val builder = AmanPopupMenuBuilder().apply(block)
    return AmanPopupMenu(title, *builder.items.toTypedArray())
}