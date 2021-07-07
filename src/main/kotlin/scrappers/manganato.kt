package scrappers

import Chapter
import Manga
import java.net.URL
import java.util.*

class Manganato : Scrapper("https://manganato.com") {
    override fun popular(): List<Manga> {
        val document = getDocument()

        return document.select(".owl-item").map { it.child(0) }.map {
            val bannerURL = it.children()[0].attr("src")
            val infoPage = it.selectFirst(".text-nowrap").attr("href")
            val name = it.children()[0].attr("alt")
            Manga(name, bannerURL, mutableListOf(), infoPage, this, UUID.randomUUID())
        }
    }

    override fun recentlyUpdated(): List<Manga> {
        val document = getDocument()

        return document.select(".content-homepage-item").map {
            val bannerURL = it.child(0).child(0).attr("src")
            val infoPage = it.child(0).attr("href")
            val name = it.selectFirst(".item-title").children()[0].ownText()
            Manga(name, bannerURL, mutableListOf(), infoPage, this, UUID.randomUUID())
        }
    }

    override fun getInfo(url: URL): Manga {
        val document = getDocument(url)

        val tableInfo = document.select(".table-value")
        var rawDescription =
            document.select(".panel-story-info-description").html().substring("<h3>Description :</h3> ".length)

        if (rawDescription.endsWith("</br>")) rawDescription = rawDescription.let {
            it.substring(it.length - "</br>".length)
        }

        val name = document.select("h1").text()
        val alternative = tableInfo[0].text().split(";")
        val cover = document.select(".info-image").select(".img-loading").attr("src")
        val author = tableInfo[1].text()
        val description = rawDescription.removeSurrounding("${'"'}")
        val genres = tableInfo[2].select("a.a-h").map { it.text() }
        val status = tableInfo[2].text()

        val manga = Manga(
            name,
            cover,
            mutableListOf(),
            url.toString(),
            this,
            UUID.randomUUID(),
            alternative,
            author,
            description,
            genres,
            status
        )

        document.select(".chapter-name").forEach {
            val text = it.text()

            val num = (Regex("[0-9.]+").find(text)?.groupValues?.get(0) ?: "0").toDouble()

            manga.addChapter(text, num, mutableListOf(), it.attr("href"))
        }

        return manga
    }

    override fun getChapter(chapter: Chapter): Chapter {

        val document = getDocument(URL(chapter.infoPage))

        document
            .selectFirst(".container-chapter-reader")
            .select("img")
            .map { it.attr("src") }
            .forEach { chapter.addPane(it) }

        return chapter
    }

    override fun search(query: String): List<Manga> {

        val document = this.getDocument("/search/story/$query")

        return document.select(".search-story-item").map {
            val bannerURL = it.child(0).child(0).attr("src")
            val infoPage = it.child(0).attr("href")
            val name = it.selectFirst(".item-title").selectFirst("a").text()

            Manga(name, bannerURL, mutableListOf(), infoPage, this, UUID.randomUUID())
        }
    }
}