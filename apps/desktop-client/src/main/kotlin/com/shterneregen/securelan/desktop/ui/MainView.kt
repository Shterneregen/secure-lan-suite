package com.shterneregen.securelan.desktop.ui

import javafx.scene.Parent

class MainView {
    private val delegate = MainViewDelegate()

    fun createContent(): Parent = delegate.createContent()

    fun shutdown() {
        delegate.shutdown()
    }
}
