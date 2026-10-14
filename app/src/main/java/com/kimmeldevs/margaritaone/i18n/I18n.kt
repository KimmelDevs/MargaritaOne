package com.kimmeldevs.margaritaone.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * In-app language switch (#27). UI text is written in English and passed through [t];
 * the English text is the lookup key into the Filipino / Waray tables. Missing entries fall back to English,
 * so a new screen never breaks — it just shows English until translated.
 */
enum class AppLanguage(val code: String, val nativeName: String) {
    ENGLISH("en", "English"),
    FILIPINO("fil", "Filipino"),
    WARAY("war", "Winaray");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

object I18n {
    fun tr(language: AppLanguage, english: String): String = when (language) {
        AppLanguage.ENGLISH -> english
        AppLanguage.FILIPINO -> FilipinoStrings[english] ?: english
        AppLanguage.WARAY -> WarayStrings[english] ?: english
    }

    /** For templates like "Make %1$s %2$s?" — translate the template, then fill it in. */
    fun tr(language: AppLanguage, english: String, vararg args: Any): String =
        tr(language, english).format(*args)
}

val LocalLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }
val LocalSetLanguage = staticCompositionLocalOf<(AppLanguage) -> Unit> { {} }

/** Translate UI text into the user's chosen language. */
@Composable
@ReadOnlyComposable
fun t(english: String): String = I18n.tr(LocalLanguage.current, english)

@Composable
@ReadOnlyComposable
fun t(english: String, vararg args: Any): String = I18n.tr(LocalLanguage.current, english, *args)
