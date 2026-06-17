package net.nullsum.freedoom.idgames

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pure JSON-text → result parsing for the idgames API, kept free of OkHttp/Android
 * so it is unit-testable on the JVM.
 *
 * Response quirks handled here: `content.file` is an array for N results but a bare
 * object for exactly 1 result; failures arrive as an `error` member; zero search
 * hits arrive as a `warning` member with no `content`.
 */
object IdgamesParser {

    fun parseFileList(jsonText: String): IdgamesResult<List<IdgamesFile>> {
        val root = idgamesJson.parseToJsonElement(jsonText).jsonObject
        root.apiError()?.let { return it }
        val content = root["content"] as? JsonObject
            ?: return IdgamesResult.Success(emptyList()) // warning / no results
        return IdgamesResult.Success(fileElementToList(content["file"]))
    }

    fun parseSingle(jsonText: String): IdgamesResult<IdgamesFile?> {
        val root = idgamesJson.parseToJsonElement(jsonText).jsonObject
        root.apiError()?.let { return it }
        val content = root["content"] as? JsonObject
            ?: return IdgamesResult.Success(null)
        // action=get returns the entry fields directly under `content`.
        return IdgamesResult.Success(idgamesJson.decodeFromJsonElement<IdgamesFile>(content))
    }

    private fun JsonObject.apiError(): IdgamesResult.ApiError? {
        val error = this["error"] ?: return null
        return when (error) {
            is JsonObject -> IdgamesResult.ApiError(
                type = error["type"]?.jsonPrimitive?.content ?: "unknown",
                message = error["message"]?.jsonPrimitive?.content ?: "Unknown API error",
            )
            JsonNull -> null
            else -> IdgamesResult.ApiError("unknown", error.jsonPrimitive.content)
        }
    }

    private fun fileElementToList(element: JsonElement?): List<IdgamesFile> = when (element) {
        null, JsonNull -> emptyList()
        is JsonArray -> element.map { idgamesJson.decodeFromJsonElement<IdgamesFile>(it) }
        is JsonObject -> listOf(idgamesJson.decodeFromJsonElement<IdgamesFile>(element))
        else -> emptyList()
    }

    /**
     * Parses the raw `reviews` value into typed reviews. The API nests them as
     * `{ "review": [...] }` and (like `content.file`) returns a single review as a
     * bare object; missing/null reviews yield an empty list. `vote` may arrive as a
     * quoted number, so it is read defensively from the primitive content.
     */
    fun parseReviews(reviews: JsonElement?): List<IdgamesReview> {
        val review = (reviews as? JsonObject)?.get("review")
        val items = when (review) {
            is JsonArray -> review.filterIsInstance<JsonObject>()
            is JsonObject -> listOf(review)
            else -> return emptyList()
        }
        return items.map { r ->
            IdgamesReview(
                text = r.string("text"),
                vote = r.string("vote")?.toIntOrNull() ?: 0,
                username = r.string("username"),
            )
        }
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
}
