package no.vaccsca.amandman.view

import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSeparator
import javax.swing.SwingConstants

class AmanPopupMenu(title: String, vararg itemGroups: AmanMenuItemData): JPopupMenu() {
    init {
        add(JLabel("<html><div style='text-align: center;'>$title</div></html>", SwingConstants.CENTER))
        add(JSeparator())
        itemGroups.forEach { itemData ->
            if (itemData.children.isNotEmpty()) {
                val submenu = JMenu(itemData.title)
                itemData.children.forEach { childData ->
                    val childItem = JMenuItem(childData.title)
                    childItem.addActionListener {
                        childData.action?.invoke()
                    }
                    submenu.add(childItem)
                }
                add(submenu)
            } else {
                val item = JMenuItem(itemData.title)
                item.addActionListener {
                    itemData.action?.invoke()
                }
                add(item)
            }
            if (itemData.separatorAfter) {
                add(JSeparator())
            }
        }
        add(JSeparator())
    }

    
}

data class AmanMenuItemData(
    val title: String,
    val children: List<AmanMenuItemData> = emptyList(),
    val separatorAfter: Boolean = false,
    val action: (() -> Unit)? = null,
)

// --- DSL Builders ---

class AmanPopupMenuBuilder(private val title: String) {
    private val items = mutableListOf<AmanMenuItemData>()

    fun section(title: String, block: AmanSectionBuilder.() -> Unit) {
        val sectionItems = AmanSectionBuilder().apply(block).build()
        items += AmanMenuItemData(
            title = title,
            children = sectionItems
        )
    }

    fun item(
        title: String,
        separatorAfter: Boolean = false,
        action: (() -> Unit)? = null
    ) {
        items += AmanMenuItemData(
            title = title,
            separatorAfter = separatorAfter,
            action = action
        )
    }

    fun build(): AmanPopupMenu = AmanPopupMenu(title, *items.toTypedArray())
}

class AmanSectionBuilder {
    private val items = mutableListOf<AmanMenuItemData>()

    fun item(
        title: String,
        separatorAfter: Boolean = false,
        action: (() -> Unit)? = null
    ) {
        items += AmanMenuItemData(
            title = title,
            separatorAfter = separatorAfter,
            action = action
        )
    }

    fun build(): List<AmanMenuItemData> = items
}

// --- Entry function ---

fun AmanPopupMenu(
    title: String,
    block: AmanPopupMenuBuilder.() -> Unit
): AmanPopupMenu {
    return AmanPopupMenuBuilder(title).apply(block).build()
}