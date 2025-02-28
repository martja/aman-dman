package org.example.presentation.tabpage

import java.awt.event.ActionEvent
import javax.swing.*

class TopBar : JPanel() {

     init {
         // Create the popup menu
         val popupMenu = JPopupMenu().apply {
             add(JMenuItem("Option 1").apply {
                 addActionListener { println("Option 1 clicked") }
             })
             add(JMenuItem("Option 2").apply {
                 addActionListener { println("Option 2 clicked") }
             })
             add(JMenuItem("Option 3").apply {
                 addActionListener { println("Option 3 clicked") }
             })
         }
         // Create a button
         val button = JButton("Open Menu").apply {
             addActionListener { e: ActionEvent ->
                 popupMenu.show(this, 0, height) // Show the popup below the button
             }
         }

         add(button)
         add(JButton("Landing Rates"))
         add(JCheckBox("Show Departures"))
         add(JCheckBox("Show Stacks"))
     }
}