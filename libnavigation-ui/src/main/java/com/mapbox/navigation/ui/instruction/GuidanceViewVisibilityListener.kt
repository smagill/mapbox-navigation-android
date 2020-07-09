package com.mapbox.navigation.ui.instruction

interface GuidanceViewVisibilityListener {
    fun onShownAt(left: Int, top: Int, width: Int, height: Int, isLandscape: Boolean)
    fun onHiden(isLandscape: Boolean)
}