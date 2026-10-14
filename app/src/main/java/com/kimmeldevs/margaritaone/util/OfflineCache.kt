package com.kimmeldevs.margaritaone.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The last copy of one screen's data saved as a JSON file, so the screen still has something
 * to show with no signal. Reading or writing never throws; a broken file just reads as nothing.
 */
class OfflineCache<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val json: Json
) {
    fun read(): T? = runCatching {
        if (file.exists()) json.decodeFromString(serializer, file.readText()) else null
    }.getOrNull()

    fun write(value: T) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(serializer, value))
        }
    }
}
