package org.example.eventHandling

interface ViewListener {
    fun onNewTabRequested(tabId: String)
    fun onOpenMetWindowClicked()
    fun refreshWeatherData(lat: Double, lon: Double)
    fun onOpenVerticalProfileWindowClicked()
    fun onAircraftSelected(callsign: String)
}