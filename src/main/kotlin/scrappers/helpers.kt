package scrappers

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import intoTextComponent
import reader
import views.MangaReader
import java.io.File
import java.util.*

data class Manga(
    val name: String,
    val bannerUrl: String,
    val chapterList: MutableList<Chapter>,
    val infoPage: String,
    val provider: Scrapper,
    var uuid: UUID,
    val alternativeNames: List<String> = listOf(),
    val author: String? = null,
    val description: String? = null,
    val genres: List<String> = listOf(),
    val status: String? = null,
    val preview: Boolean = false
) {
    fun addChapter(name: String, number: Double, panes: MutableList<Pane>, infoPage: String) {
        chapterList.add(Chapter(name, number, this, panes, UUID.randomUUID(), infoPage))
    }

    fun toProperties() = Properties().apply {
        setProperty("name", name)
        setProperty("bannerUrl", bannerUrl)
        setProperty("infoPage", infoPage)
        setProperty("chapterList", chapterList.joinToString("|") { it.uuid.toString() })
        setProperty("source", provider.url)
        setProperty("uuid", uuid.toString())
        setProperty("alternativeNames", alternativeNames.joinToString("|"))
        setProperty("author", author ?: "")
        setProperty("description", description ?: "")
        setProperty("genres", genres.joinToString("|"))
        setProperty("status", status ?: "")
        setProperty("preview", "$preview")
    }

    fun getDir() = File(reader.defaultMangaDir, uuid.toString())
    fun getInfo() = File(getDir(), "info.props")

    @Composable
    fun createPreview() {
        Card(elevation = 5.dp, modifier = Modifier.clickable { reader.viewersManager.open(this) }) {
            Column {
                if (bannerUrl.isNotBlank()) {
                    val bytes = reader.getImage(
                        bannerUrl,
                        file = File(reader.defaultImageDir, "${uuid}.${bannerUrl.split(".").last()}")
                    )
                    Image(bytes, name, Modifier.size(200.dp, 310.dp))
                }

                name.intoTextComponent()
            }
        }
    }

    companion object {
        fun createFromProperties(props: Properties) = Manga(
            name = props.getProperty("name"),
            bannerUrl = props.getProperty("bannerUrl"),
            chapterList = mutableStateListOf(),
            infoPage = props.getProperty("infoPage"),
            provider = MangaReader.providers.find { it.url == props.getProperty("source") }!!,
            uuid = UUID.fromString(props.getProperty("uuid")),
            alternativeNames = props.getProperty("alternativeNames").split("|"),
            author = props.getProperty("author"),
            description = props.getProperty("description"),
            genres = props.getProperty("genres").split("|"),
            status = props.getProperty("status"),
            preview = props.getProperty("preview") == "true"
        )
    }
}

data class Chapter(
    val name: String,
    val chapterNumber: Double,
    val manga: Manga,
    val panes: MutableList<Pane>,
    var uuid: UUID,
    val infoPage: String,
    var userStatus: UserStatus = UserStatus.NONE,
    var lastPage: Int = 0
) {
    fun addPane(url: String) {
        panes.add(Pane(url, this))
    }

    fun toProperties() = Properties().apply {
        setProperty("name", name)
        setProperty("number", chapterNumber.toString())
        setProperty("uuid", uuid.toString())
        setProperty("panes", panes.joinToString("|") { it.url })
        setProperty("infoPage", infoPage)
        setProperty("userStatus", userStatus.ordinal.toString())
        setProperty("lastPage", lastPage.toString())
    }

    fun getDir() = File(manga.getDir(), uuid.toString())
    fun getInfo() = File(getDir(), "info.props")

    companion object {
        fun createFromProperties(props: Properties, manga: Manga) = Chapter(
            name = props.getProperty("name"),
            chapterNumber = props.getProperty("number").toDouble(),
            manga = manga,
            panes = mutableListOf(),
            uuid = UUID.fromString(props.getProperty("uuid")),
            infoPage = props.getProperty("infoPage"),
            userStatus = props.getProperty("userStatus")
                .let { prop ->
                    UserStatus.values()
                        .find { it.ordinal == prop.toInt() }
                }!!,
            lastPage = props.getProperty("lastPage").toInt()
        ).apply {
            panes.addAll(
                props.getProperty("panes").split("|").mapNotNull { if (it.isEmpty()) null else Pane(it, this) }
            )
        }
    }
}

data class Pane(val url: String, val chapter: Chapter)
enum class UserStatus { NONE, READING, DONE }