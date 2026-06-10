package net.nullsum.freedoom.idgames

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A file entry as returned by the Doomworld /idgames archive API
 * (https://www.doomworld.com/idgames/api/). The API is a PHP wrapper, so field
 * presence and types drift between entries — everything optional gets a default.
 */
@Serializable
data class IdgamesFile(
    val id: Long = 0,
    val title: String? = null,
    val dir: String = "",
    val filename: String = "",
    val size: Long = 0,
    val date: String? = null,
    val author: String? = null,
    val description: String? = null,
    val rating: Double = 0.0,
    val votes: Int = 0,
) {
    /** Install dir name under wads/: the zip filename minus its extension. */
    val installName: String get() = filename.substringBeforeLast('.')
}

sealed interface IdgamesResult<out T> {
    data class Success<T>(val value: T) : IdgamesResult<T>
    data class ApiError(val type: String, val message: String) : IdgamesResult<Nothing>
}

/** Shared lenient decoder for the whole idgames layer. */
internal val idgamesJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}
