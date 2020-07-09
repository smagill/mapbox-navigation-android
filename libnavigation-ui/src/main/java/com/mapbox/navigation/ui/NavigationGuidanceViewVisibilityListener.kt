package com.mapbox.navigation.ui

import com.mapbox.navigation.ui.instruction.GuidanceViewVisibilityListener

internal class NavigationGuidanceViewVisibilityListener(private val navigationPresenter: NavigationPresenter) :
    GuidanceViewVisibilityListener {
    override fun onShownAt(left: Int, top: Int, width: Int, height: Int, isLandscape: Boolean) {
        navigationPresenter.onGuidanceViewChange(left, top, width, height, isLandscape)
    }

    override fun onHiden(isLandscape: Boolean) {
        navigationPresenter.onGuidanceViewChange(0, 0, 0,0 , isLandscape)
    }
}