package com.msa.freedoom.idgames

/**
 * HTTPS mirrors of the /idgames archive file tree. A download is
 * `mirror + dir + filename`; mirrors are tried in order until one works.
 */
object IdgamesMirrors {

    val MIRRORS = listOf(
        "https://youfailit.net/pub/idgames/",
        "https://www.quaddicted.com/files/idgames/",
        "https://ftpmirror.infania.net/pub/idgames/",
        "https://www.gamers.org/pub/idgames/",
    )

    fun downloadUrl(mirrorBase: String, dir: String, filename: String): String =
        joinUrl(joinUrl(mirrorBase, dir), filename)

    private fun joinUrl(left: String, right: String): String =
        left.trimEnd('/') + "/" + right.trim('/')
}
