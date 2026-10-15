package com.kimmeldevs.margaritaone.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// MargaritaOne palette (design kit): red for actions/alerts, blue for navigation/links, deep navy for headings.
val PrimaryRed = Color(0xFFE31837)
val PrimaryBlue = Color(0xFF0866C6)
val DeepNavy = Color(0xFF0D3157)
val CoolWhite = Color(0xFFF4F7FB)
val LightGray = Color(0xFFE4EAF2)
val SlateGray = Color(0xFF64748B)
val SoftCoral = Color(0xFFFCA5A5)
private val GradientMiddle = Color(0xFFB51746)
private val BackgroundEnd = Color(0xFFDCE8F7)

/** Signature brand gradient (red → crimson → blue): primary buttons, logo accents, selected highlights. */
val BrandGradient: Brush get() = Brush.horizontalGradient(listOf(PrimaryRed, GradientMiddle, PrimaryBlue))

/** Clean background gradient: login screen and large content areas. */
val BackgroundGradient: Brush get() = Brush.linearGradient(listOf(CoolWhite, BackgroundEnd))

/** Emergency gradient: emergency hotline card and alerts only. */
val EmergencyGradient: Brush get() = Brush.linearGradient(listOf(PrimaryRed, SoftCoral))

// Light scheme
val LightPrimary = PrimaryRed
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFE1E5)
val LightOnPrimaryContainer = Color(0xFF5E0012)
val LightSecondary = PrimaryBlue
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFDCEBFB)
val LightOnSecondaryContainer = Color(0xFF032B55)
val LightTertiary = DeepNavy
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFDDE5F0)
val LightOnTertiaryContainer = DeepNavy

// Dark scheme (same hues, lifted for contrast on a navy-black background)
val DarkPrimary = Color(0xFFFF8A98)
val DarkOnPrimary = Color(0xFF5E0012)
val DarkPrimaryContainer = Color(0xFF8E0F25)
val DarkOnPrimaryContainer = Color(0xFFFFDADF)
val DarkSecondary = Color(0xFF8EC2FF)
val DarkOnSecondary = Color(0xFF00305C)
val DarkSecondaryContainer = Color(0xFF004B8C)
val DarkOnSecondaryContainer = Color(0xFFD3E5FF)
val DarkTertiary = Color(0xFFB8C8E2)
val DarkOnTertiary = Color(0xFF1B2F4B)
val DarkTertiaryContainer = Color(0xFF2B4264)
val DarkOnTertiaryContainer = Color(0xFFDDE5F0)
val DarkBackground = Color(0xFF0B1322)
val DarkSurface = Color(0xFF111B2D)
val DarkSurfaceVariant = Color(0xFF17243A)
val DarkOnSurface = Color(0xFFE7EDF6)
val DarkOnSurfaceVariant = Color(0xFFA9B5C8)
val DarkOutline = Color(0xFF6E7D93)
val DarkOutlineVariant = Color(0xFF2A3A52)
