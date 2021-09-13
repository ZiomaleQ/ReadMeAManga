package scrappers

import Chapter
import Manga
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

abstract class Scrapper(val url: String) {
    private val cache = HashMap<String, String>()

    abstract fun popular(): List<Manga>
    abstract fun recentlyUpdated(): List<Manga>
    abstract fun getInfo(url: URL, refresh: Boolean = false): Manga
    abstract fun getChapter(chapter: Chapter, refresh: Boolean = false): Chapter
    abstract fun search(query: String): List<Manga>

    fun getDocument(path: String = "", refresh: Boolean = false): Document =
        this.getDocument(URL("$url${if (path.isEmpty()) "" else "/${path}"}"), refresh)

    fun getDocument(path: URL, refresh: Boolean = false): Document {

        val tempUrl = path.toString()

        var rawHtml = cache[tempUrl]

        if (rawHtml.isNullOrEmpty() || refresh) {
            rawHtml = path.openStream()
                .use { it.readAllBytes() }
                .let { String(it, Charsets.UTF_8) }
                .also { cache[tempUrl] = it }
        }

        return Jsoup.parse(rawHtml)
    }
}