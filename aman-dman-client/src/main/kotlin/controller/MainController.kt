package org.example.controller

import integration.AtcClientEuroScope
import org.example.integration.AtcClient
import org.example.model.TabState
import org.example.presentation.AmanDmanMainFrame
import org.example.state.ApplicationState
import org.example.view.TabView

class MainController {

    private var atcClient: AtcClient? = null
    private var mainWindow: AmanDmanMainFrame? = null
    private val applicationState = ApplicationState()

    fun startApplication() {
        mainWindow = AmanDmanMainFrame(applicationState, this)
        mainWindow?.isVisible = true
        atcClient = AtcClientEuroScope("127.0.0.1", 12345)

        createNewTab("Tab 1")
        createNewTab("Tab 2")
    }

    fun createNewTab(name: String) {
        val tabState = TabState(applicationState)
        val tabController = TabController(applicationState, tabState, atcClient!!)
        val tabView = TabView(tabController, tabState)
        tabController.setView(tabView)
        mainWindow?.addTab(name, tabView)
    }
}