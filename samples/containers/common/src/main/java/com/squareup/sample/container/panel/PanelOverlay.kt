package com.squareup.sample.container.panel

import com.squareup.workflow1.ui.Screen
import com.squareup.workflow1.ui.navigation.ModalOverlay
import com.squareup.workflow1.ui.navigation.ScreenOverlay

class PanelOverlay<out C : Screen>(
  override val content: C
) : ScreenOverlay<C>, ModalOverlay {
  override fun <D : Screen> map(transform: (C) -> D): PanelOverlay<D> =
    PanelOverlay(transform(content))
}
