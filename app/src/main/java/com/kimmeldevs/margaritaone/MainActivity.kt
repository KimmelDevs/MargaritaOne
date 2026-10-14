package com.kimmeldevs.margaritaone

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kimmeldevs.margaritaone.i18n.LocalLanguage
import com.kimmeldevs.margaritaone.i18n.LocalSetLanguage
import com.kimmeldevs.margaritaone.navigation.MargaritaOneRoot
import com.kimmeldevs.margaritaone.ui.theme.LocalSetThemeMode
import com.kimmeldevs.margaritaone.ui.theme.LocalThemeMode
import com.kimmeldevs.margaritaone.ui.theme.MargaritaOneTheme
import com.kimmeldevs.margaritaone.util.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val localStore = (application as MargaritaOneApp).container.localStore
        setContent {
            var language by remember { mutableStateOf(localStore.language) }
            var themeMode by remember { mutableStateOf(localStore.themeMode) }
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Keep status/navigation bar icons readable when the app theme differs from the phone's.
            DisposableEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            CompositionLocalProvider(
                LocalLanguage provides language,
                LocalSetLanguage provides { chosen ->
                    language = chosen
                    localStore.language = chosen
                },
                LocalThemeMode provides themeMode,
                LocalSetThemeMode provides { chosen ->
                    themeMode = chosen
                    localStore.themeMode = chosen
                }
            ) {
                MargaritaOneTheme(darkTheme = darkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MargaritaOneRoot()
                    }
                }
            }
        }
    }
}
