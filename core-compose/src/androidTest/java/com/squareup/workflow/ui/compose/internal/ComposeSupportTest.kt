/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry")

package com.squareup.workflow.ui.compose.internal

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.compose.CompositionReference
import androidx.compose.FrameManager
import androidx.compose.Providers
import androidx.compose.Recomposer
import androidx.compose.ambientOf
import androidx.compose.compositionReference
import androidx.compose.currentComposer
import androidx.compose.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.ui.foundation.Text
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeSupportTest {

  @Rule @JvmField val composeRule = createComposeRule()

  @Test fun ambientsPassThroughSubcomposition() {
    composeRule.setContent {
      TestComposable("foo")
    }

    findByText("foo").assertIsDisplayed()
  }

  @Test fun ambientChangesPassThroughSubcomposition() {
    val ambientValue = mutableStateOf("foo")
    composeRule.setContent {
      TestComposable(ambientValue.value)
    }

    findByText("foo").assertIsDisplayed()
    FrameManager.framed {
      ambientValue.value = "bar"
    }
    findByText("bar").assertIsDisplayed()
  }

  @Composable private fun TestComposable(ambientValue: String) {
    Providers(TestAmbient provides ambientValue) {
      LegacyHostComposable {
        Text(TestAmbient.current)
      }
    }
  }

  @Composable private fun LegacyHostComposable(leafContent: @Composable() () -> Unit) {
    val wormhole = Wormhole(currentComposer.recomposer, compositionReference(), leafContent)
    // This is valid Compose code, but the IDE doesn't know that yet so it will show an
    // unsuppressable error.
    WormholeView(wormhole = wormhole)
  }

  private class Wormhole(
    val recomposer: Recomposer,
    val parentReference: CompositionReference,
    val childContent: @Composable() () -> Unit
  )

  private class WormholeView(context: Context) : FrameLayout(context) {
    fun setWormhole(wormhole: Wormhole) {
      setContent(wormhole.recomposer, wormhole.parentReference, wormhole.childContent)
    }
  }

  private companion object {
    val TestAmbient = ambientOf<String> { error("Ambient not provided") }
  }
}
