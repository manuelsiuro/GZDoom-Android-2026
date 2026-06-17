package net.nullsum.freedoom.idgames

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * A file entry as returned by the Doomworld /idgames archive API
 * (https://www.doomworld.com/idgames/api/). The API is a PHP wrapper, so field
 * presence and types drift between entries — everything optional gets a default.
 *
 * Search/latest results carry only the lightweight fields; the heavy fields
 * ([textfile], [credits], [reviews], …) are populated by the `get` action and
 * power the rich detail sheet.
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
    // --- richer fields, present on `get` responses ---
    val age: Long? = null,
    val email: String? = null,
    val credits: String? = null,
    val base: String? = null,
    val buildtime: String? = null,
    val editors: String? = null,
    val bugs: String? = null,
    val textfile: String? = null,
    val url: String? = null,
    val idgamesurl: String? = null,
    /**
     * Raw `reviews` value: the API nests them as `{ "review": [...] }` and may
     * return a single review as a bare object, so it can't be auto-decoded. Use
     * [reviewList] (parsed via [IdgamesParser.parseReviews]).
     */
    val reviews: JsonElement? = null,
) {
    /** Install dir name under wads/: the zip filename minus its extension. */
    val installName: String get() = filename.substringBeforeLast('.')

    /** Parsed reviews, handling the array/object/missing drift. */
    val reviewList: List<IdgamesReview> get() = IdgamesParser.parseReviews(reviews)
}

/** A single user review, as nested under `content.reviews.review`. */
@Serializable
data class IdgamesReview(
    val text: String? = null,
    val vote: Int = 0,
    val username: String? = null,
)

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
