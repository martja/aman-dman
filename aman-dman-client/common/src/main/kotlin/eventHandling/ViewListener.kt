package org.example.eventHandling

interface ViewListener {
    fun onNewTabRequested(tabId: String)
    fun onOpenMetWindowClicked()
}