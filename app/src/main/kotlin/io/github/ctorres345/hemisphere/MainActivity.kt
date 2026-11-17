package io.github.ctorres345.hemisphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.feature.browser.BrowserRoute
import io.github.ctorres345.hemisphere.feature.browser.BrowserViewModel

/** The only Activity. Shows the Browser screen; Navigation 3 adds the other screens later. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as HemisphereApplication).container
        val browserFactory =
            viewModelFactory {
                initializer { BrowserViewModel(container.getHomeFolder, container.observeFolder) }
            }
        setContent {
            HemisphereTheme {
                BrowserRoute(viewModel = viewModel(factory = browserFactory))
            }
        }
    }
}
