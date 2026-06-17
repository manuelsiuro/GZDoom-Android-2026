package com.msa.freedoom.idgames

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import com.msa.freedoom.BuildConfig
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

    /** `get` by archive file path (e.g. "levels/doom2/megawads/scythe.zip"). */
    suspend fun get(filePath: String): IdgamesResult<IdgamesFile?> {
        val body = fetch {
            addQueryParameter("action", "get")
            addQueryParameter("file", filePath)
        }
        return IdgamesParser.parseSingle(body)
    }

    /** Resolves a parsed idgames:// reference to its full file record. */
    suspend fun resolve(ref: IdgamesUri.Ref): IdgamesResult<IdgamesFile?> = when (ref) {
        is IdgamesUri.Ref.ById -> get(ref.id)
        is IdgamesUri.Ref.ByPath -> get(ref.filePath)
    }

    /**
     * Lists a directory of the idgames archive file tree (gamers.org mirror) by
     * parsing its Apache HTML index. [path] is relative to the archive root, e.g.
     * "levels/doom2". A missing directory parses to an empty list.
     */
    suspend fun listArchive(path: String): List<ArchiveListingParser.Node> {
        val url = ARCHIVE_BASE.toHttpUrl().newBuilder().apply {
            path.split('/').filter { it.isNotBlank() }.forEach { addPathSegment(it) }
            addPathSegment("") // trailing slash so Apache serves the directory index
        }.build()
        return ArchiveListingParser.parse(fetchUrl(url))
    }

    private suspend fun fetch(params: okhttp3.HttpUrl.Builder.() -> Unit): String {
        val url = API_BASE.toHttpUrl().newBuilder()
            .apply(params)
            .addQueryParameter("out", "json")
            .build()
        return fetchUrl(url)
    }

    private suspend fun fetchUrl(url: okhttp3.HttpUrl): String {
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

        // File-tree mirror used for the archive-tree browser (HTML directory index).
        const val ARCHIVE_BASE = "https://www.gamers.org/pub/idgames/"

        // The API host asks clients to identify themselves with a descriptive UA.
        val USER_AGENT =
            "Freedoom-for-Android/${BuildConfig.VERSION_NAME} (com.msa.freedoom; WAD browser)"

        val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                // Without explicit timeouts a stalled mirror hangs the download coroutine
                // until the user manually cancels. readTimeout fires on per-read inactivity,
                // so it kills a stalled connection without capping a steady large download —
                // hence no callTimeout (which is absolute wall-clock and would abort big WADs).
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder().header("User-Agent", USER_AGENT).build(),
                    )
                }
                .build()
        }
    }
}
