package com.kimmeldevs.margaritaone

import com.kimmeldevs.margaritaone.i18n.FilipinoStrings
import com.kimmeldevs.margaritaone.i18n.WarayStrings
import org.junit.Assert.assertEquals
import org.junit.Test

/** A translation with different %s / %d / %1$s placeholders than its English key would crash String.format. */
class TranslationsTest {

    private val placeholder = Regex("""%(\d+\$)?[sd]""")

    private fun placeholders(s: String) = placeholder.findAll(s).map { it.value }.sorted().toList()

    private fun check(name: String, table: Map<String, String>) {
        table.forEach { (english, translated) ->
            assertEquals("$name placeholders differ for \"$english\"", placeholders(english), placeholders(translated))
        }
    }

    @Test
    fun filipinoPlaceholdersMatch() = check("Filipino", FilipinoStrings)

    @Test
    fun warayPlaceholdersMatch() = check("Waray", WarayStrings)
}
