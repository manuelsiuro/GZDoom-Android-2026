package net.nullsum.freedoom.idgames

/**
 * Parses the community `idgames://` deep-link scheme. The Doomworld API emits a
 * canonical `idgamesurl` per file; in practice the link body is either the numeric
 * file id (`idgames://12345`) or the archive file path
 * (`idgames://levels/doom2/megawads/scythe.zip`). Both resolve via the API `get`
 * action (by id or by `file=`), see [IdgamesApi.resolve].
 */
object IdgamesUri {

    sealed interface Ref {
        data class ById(val id: Long) : Ref
        data class ByPath(val filePath: String) : Ref
    }

    private const val SCHEME = "idgames://"

    fun parse(uri: String): Ref? {
        if (!uri.startsWith(SCHEME, ignoreCase = true)) return null
        val body = uri.substring(SCHEME.length)
            .substringBefore('?')
            .substringBefore('#')
            .trim()
            .trimStart('/')
        if (body.isEmpty()) return null
        body.toLongOrNull()?.let { return Ref.ById(it) }
        return Ref.ByPath(body)
    }
}
