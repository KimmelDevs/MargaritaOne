package com.kimmeldevs.margaritaone.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.kimmeldevs.margaritaone.util.ThemeMode

/** Current light/dark choice and a setter, provided by MainActivity. */
val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }
val LocalSetThemeMode = staticCompositionLocalOf<(ThemeMode) -> Unit> { {} }
