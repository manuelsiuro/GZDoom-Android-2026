package net.nullsum.freedoom.idgames

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import net.nullsum.freedoom.BuildConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** HTTP client for the Doomworld /idgames archive API. */
class IdgamesApi(private val client: OkHttpClient = sharedClient) {

    enum class SearchType(val param: String) {
        TITLE("title"),
        FILENAME("filename"),
        AUTHOR("author"),
        DESCRIPTION("description"),
    }

    suspend fun search(
        query: String,
        type: SearchType,
        sort: String = "rating",
    ): IdgamesResult<List<IdgamesFile>> {
        val body = fetch {
            addQueryParameter("action", "search")
            addQueryParameter("query", query)
            addQueryParameter("type", type.param)
            addQueryParameter("sort", sort)
            addQueryParameter("dir", "desc")
        }
        return IdgamesParser.parseFileList(body)
    }

    suspend fun latest(limit: Int = 20): IdgamesResult<List<IdgamesFile>> {
        val body = fetch {
            addQueryParameter("action", "latestfiles")
            addQueryParameter("limit", limit.toString())
        }
        return IdgamesParser.parseFileList(body)
    }

    suspend fun get(id: Long): IdgamesResult<IdgamesFile?> {
        val body = fetch {
            addQueryParameter("action", "get")
            addQueryParameter("id", id.toString())
        }
        return IdgamesParser.parseSingle(body)
    }

    private suspend fun fetch(params: okhttp3.HttpUrl.Builder.() -> Unit): String {
        val url = API_BASE.toHttpUrl().newBuilder()
            .apply(params)
            .addQueryParameter("out", "json")
            .build()
        val call = client.newCall(Request.Builder().url(url).build())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(IOException("HTTP ${it.code}"))
                            }
                            return
                        }
                        val body = it.body?.string().orEmpty()
                        if (continuation.isActive) continuation.resume(body)
                    }
                }
            })
        }
    }

    companion object {
        const val API_BASE = "https://www.doomworld.com/idgames/api/api.php"

        // The API host asks clients to identify themselves with a descriptive UA.
        val USER_AGENT =
            "Freedoom-for-Android/${BuildConfig.VERSION_NAME} (net.nullsum.freedoom; WAD browser)"

        val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder().header("User-Agent", USER_AGENT).build(),
                    )
                }
                .build()
        }
    }
}
