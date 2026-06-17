package net.nullsum.freedoom.idgames

/**
 * Parses an Apache `mod_autoindex` HTML directory listing (as served by the
 * gamers.org idgames mirror) into nodes. Tolerant of both the `<pre>` and table
 * layouts: it keys off the `<a href>` anchors and pulls the trailing date/size from
 * the rest of the row. A page with no listing rows (e.g. a styled 404 served with
 * HTTP 200) parses to an empty list rather than throwing.
 */
object ArchiveListingParser {

    data class Node(
        val name: String,
        val isDir: Boolean,
        val sizeBytes: Long?,
        val lastModified: String?,
    )

    private val LINK_RE = Regex("""<a\s+href="([^"]+)"[^>]*>(.*?)</a>(.*)""", RegexOption.IGNORE_CASE)
    private val TAG_RE = Regex("""<[^>]*>""")
    private val DATE_RE = Regex("""\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}|\d{1,2}-[A-Za-z]{3}-\d{4}\s+\d{2}:\d{2}""")
    private val SIZE_RE = Regex("""^\d+(\.\d+)?[KMGT]?$""", RegexOption.IGNORE_CASE)

    fun parse(html: String): List<Node> = html.lineSequence().mapNotNull { line ->
        val m = LINK_RE.find(line) ?: return@mapNotNull null
        val href = m.groupValues[1]
        // Skip column-sort query links, parent/self links and absolute/protocol hrefs.
        if (href.startsWith("?") || href.startsWith("/") || "://" in href ||
            href == "../" || href.startsWith("..")
        ) return@mapNotNull null
        val label = TAG_RE.replace(m.groupValues[2], "").trim()
        if (label.isEmpty() || label.equals("Parent Directory", ignoreCase = true)) return@mapNotNull null

        val isDir = href.endsWith("/")
        val name = href.trimEnd('/').substringAfterLast('/')
        if (name.isEmpty()) return@mapNotNull null

        val tail = TAG_RE.replace(m.groupValues[3], " ").trim()
        val date = DATE_RE.find(tail)?.value
        val sizeToken = tail.split(Regex("""\s+""")).lastOrNull()
        Node(name, isDir, if (isDir) null else parseSize(sizeToken), date)
    }.toList()

    /** Apache size tokens: "-", "1.2K", "3.4M", "2G", or a plain byte count. */
    private fun parseSize(token: String?): Long? {
        if (token == null || token == "-") return null
        if (!SIZE_RE.matches(token)) return null
        val suffix = token.last().uppercaseChar()
        val mult = when (suffix) {
            'K' -> 1L shl 10
            'M' -> 1L shl 20
            'G' -> 1L shl 30
            'T' -> 1L shl 40
            else -> 1L
        }
        val number = if (mult == 1L) token else token.dropLast(1)
        val value = number.toDoubleOrNull() ?: return null
        return (value * mult).toLong()
    }
}
