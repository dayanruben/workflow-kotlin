@file:OptIn(WorkflowExperimentalRuntime::class)

package com.squareup.sample.compose.hellocomposebinding

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.workflow1.WorkflowExperimentalRuntime
import com.squareup.workflow1.android.renderWorkflowIn
import com.squareup.workflow1.config.AndroidRuntimeConfigTools
import com.squareup.workflow1.mapRendering
import com.squareup.workflow1.ui.Screen
import com.squareup.workflow1.ui.ViewEnvironment
import com.squareup.workflow1.ui.ViewRegistry
import com.squareup.workflow1.ui.compose.withComposeInteropSupport
import com.squareup.workflow1.ui.plus
import com.squareup.workflow1.ui.withEnvironment
import com.squareup.workflow1.ui.workflowContentView
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.plus

private val viewEnvironment =
  (ViewEnvironment.EMPTY + ViewRegistry(HelloBinding))
    .withComposeInteropSupport { content ->
      MaterialTheme(content = content)
    }

/**
 * Demonstrates how to create and display a view factory with
 * [screenComposableFactory][com.squareup.workflow1.ui.compose.ScreenComposableFactory].
 */
class HelloBindingActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val model: HelloBindingModel by viewModels()
    workflowContentView.take(
      lifecycle = lifecycle,
      renderings = model.renderings,
    )
  }

  class HelloBindingModel(savedState: SavedStateHandle) : ViewModel() {
    val renderings: StateFlow<Screen> by lazy {
      renderWorkflowIn(
        workflow = HelloWorkflow.mapRendering { it.withEnvironment(viewEnvironment) },
        scope = viewModelScope + AndroidUiDispatcher.Main,
        savedStateHandle = savedState,
        runtimeConfig = AndroidRuntimeConfigTools.getAppWorkflowRuntimeConfig()
      )
    }
  }
}
