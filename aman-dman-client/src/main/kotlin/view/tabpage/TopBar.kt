package org.example.presentation.tabpage

import javax.swing.*

class TopBar : JPanel() {

     init {
         add(JButton("Landing Rates"))
         add(JCheckBox("Show Departures"))
         add(JCheckBox("Show Stacks"))
     }
}