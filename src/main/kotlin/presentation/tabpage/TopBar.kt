package org.example.presentation.tabpage

import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JPanel

class TopBar : JPanel() {

     init {
         add(JButton("NonSeq"))
         add(JButton("Landing Rates"))
         add(JButton("Search"))
         add(JCheckBox("Show Departures"))
         add(JCheckBox("Show Stacks"))
     }
}