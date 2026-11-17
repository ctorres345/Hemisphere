package io.github.ctorres345.hemisphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme

/** The only Activity. Hosts the app back stack ([HemisphereNavHost]). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HemisphereTheme {
                HemisphereNavHost()
            }
        }
    }
}
