package dev.aiaerial.signal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.aiaerial.signal.data.openclaw.OpenClawClient
import dev.aiaerial.signal.ui.navigation.SignalNavHost
import dev.aiaerial.signal.ui.theme.SignalTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var openClawClient: OpenClawClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignalTheme {
                SignalNavHost(openClawClient = openClawClient)
            }
        }
    }
}
