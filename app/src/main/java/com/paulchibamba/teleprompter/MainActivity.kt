package com.paulchibamba.teleprompter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.paulchibamba.teleprompter.ui.navigation.PrompterNavHost
import com.paulchibamba.teleprompter.ui.theme.PrompterTheme

/**
 * The only activity. Everything inside it is Compose navigation, which matters later: remote key
 * events arrive through `dispatchKeyEvent`, and a single activity gives that exactly one home.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrompterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PrompterNavHost()
                }
            }
        }
    }
}
