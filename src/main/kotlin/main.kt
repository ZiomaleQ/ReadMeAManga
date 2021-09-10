import androidx.compose.desktop.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import scrappers.Manganato
import scrappers.Scrapper
import views.MangaReader
import java.io.File
import java.net.URL
import java.util.*

val reader = MangaReader()

fun main() = Window(
    title = "Read Me A Manga",
    size = IntSize(1280, 768)
) {
    MaterialTheme { reader.createView() }
}

val mangaList = Manganato().recentlyUpdated().let {
    it.subList(0, it.size - (it.size % reader.settings.mangaPerRow))
}

@Composable
fun MangaBrowser() {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(10.dp).fillMaxWidth()) {
            "Searching for something? ".intoTextComponent()
            var text by remember { mutableStateOf("") }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.size(100.dp, 36.dp).padding(4.dp).border(2f, BorderSide.BOTTOM)
            )
            Icon(
                Icons.Default.PlayArrow,
                tint = LocalContentColor.current,
                contentDescription = "Search",
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp)
                    .clickable {
                        reader.viewersManager.open(SearchPage(text))
                    }
            )
        }
        for (i in 0..(Math.floorDiv(mangaList.size, reader.settings.mangaPerRow))) {
            Row {
                for (manga in mangaList.subList(
                    i * reader.settings.mangaPerRow,
                    (((i + 1) * reader.settings.mangaPerRow)).coerceAtMost(mangaList.size)
                )) {
                    Box(Modifier.padding(10.dp)) { mangaPreview(manga) }
                }
            }
        }
    }
}

@Composable
fun ViewersTabView(model: Viewer) = Surface(
    color = if (model.isActive) Color.LightGray
    else Color.Transparent
) {
    Row(
        Modifier
            .clickable(remember { MutableInteractionSource() }, indication = null) { model.activate() }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        model.title.intoTextComponent(Modifier.padding(horizontal = 4.dp))

        if ((model as? CloseableViewer)?.close != null) {
            Icon(
                Icons.Default.Close,
                tint = LocalContentColor.current,
                contentDescription = "Close",
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp)
                    .clickable {
                        model.close?.let { it() }
                    }
            )
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp, 24.dp)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun ChapterInfo(model: ChapterViewer) = Row(Modifier.fillMaxWidth()) {

    val chapter = if (model.chapter.panes.isNotEmpty()) model.chapter else Manganato().getChapter(model.chapter)

    var pageNumber by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxHeight()) {
        Icon(
            Icons.Default.KeyboardArrowLeft,
            tint = LocalContentColor.current,
            contentDescription = "Left",
            modifier = Modifier.padding(4.dp).clickable {
                if (pageNumber > 0) pageNumber -= 1
            }.fillMaxWidth(0.05f)
        )

        Icon(
            Icons.Default.ArrowBack,
            tint = LocalContentColor.current,
            contentDescription = "Go back (earlier chapter)",
            modifier = Modifier.padding(4.dp).clickable {
                val list = model.chapter.manga.chapterList
                val index = list.indexOf(model.chapter)

                //List is in reversed order
                if (index == -1 || index + 1 >= list.size) return@clickable

                val earlierChapter = list[index + 1]
                reader.viewersManager.update(model, ChapterViewer(earlierChapter))

            }.fillMaxWidth(0.05f)
        )
    }

    val referer = URL(chapter.infoPage).let { "${it.protocol}://${it.host}" }

    val bytes = reader.getImage(
        chapter.panes[pageNumber].url,
        referer = referer,
        File(chapter.getDir(), "$pageNumber.${chapter.panes[pageNumber].url.split(".").last()}")
    )
    Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).weight(1f)) {
        Image(bytes, chapter.name, Modifier.border(1.dp, Color.Blue).fillMaxWidth())
    }

    Column(Modifier.fillMaxHeight()) {
        Icon(
            Icons.Default.KeyboardArrowRight,
            tint = LocalContentColor.current,
            contentDescription = "Right",
            modifier = Modifier.padding(4.dp).clickable {
                if (pageNumber < (chapter.panes.size - 1)) pageNumber += 1
            }.fillMaxWidth(0.05f)
        )

        Icon(
            Icons.Default.ArrowForward,
            tint = LocalContentColor.current,
            contentDescription = "Go forward (next chapter)",
            modifier = Modifier.padding(4.dp).clickable {
                val list = model.chapter.manga.chapterList
                //List is in reversed order
                val index = list.indexOf(model.chapter)

                if (index == -1 || list.size - index <= 0) return@clickable

                val earlierChapter = list[index - 1]
                reader.viewersManager.update(model, ChapterViewer(earlierChapter))

            }.fillMaxWidth(0.05f)
        )
    }

}

@Composable
fun SearchResult(model: SearchPage) = Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
    val results = Manganato().search(model.query)

    "Search page for ${model.query}".intoTextComponent()

    for (i in 0..(Math.floorDiv(results.size, reader.settings.mangaPerRow))) {
        Row {
            for (manga in results.subList(
                i * reader.settings.mangaPerRow,
                (((i + 1) * reader.settings.mangaPerRow)).coerceAtMost(results.size)
            )) {
                Box(Modifier.padding(10.dp)) { mangaPreview(manga) }
            }
        }
    }
}

@Composable
fun MangaInfo(model: MangaViewer) = Row {

    val manga = model.manga.provider.getInfo(URL(model.manga.infoPage))

    Column(Modifier.fillMaxWidth(0.25f).padding(horizontal = 10.dp)) {

        if (manga.bannerUrl.isNotBlank()) {
            val bytes = reader.getImage(manga.bannerUrl)
            Image(bytes, manga.name, Modifier.border(1.dp, Color.Blue).size(200.dp, 310.dp))
        }

        manga.name.intoTextComponent(Modifier.padding(vertical = 5.dp))

        Row {

            Icon(
                Icons.Default.Add,
                tint = LocalContentColor.current,
                contentDescription = "Add to library",
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp)
                    .clickable {
                        println("Add to library")
                    }
            )

            Spacer(Modifier.width(8.dp))

            Icon(
                Icons.Default.Refresh,
                tint = LocalContentColor.current,
                contentDescription = "Refresh info",
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp)
                    .clickable {
                        println("Refresh manga")
                    }
            )

            Spacer(Modifier.width(8.dp))

            Icon(
                Icons.Default.ArrowDropDown,
                tint = LocalContentColor.current,
                contentDescription = "Download",
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp)
                    .clickable {
                        println("Download chapter(s)")
                    }
            )
        }

        Column(Modifier.fillMaxHeight().padding(vertical = 10.dp).verticalScroll(rememberScrollState())) {
            (manga.description ?: "No description").intoTextComponent()
        }
    }

    Column(Modifier.fillMaxWidth(1f).padding(10.dp)) {

        "Manga chapters: ${manga.chapterList.size}".intoTextComponent()

        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            manga.chapterList.forEach {
                it.name.intoTextComponent(Modifier.padding(10.dp).clickable {
                    reader.viewersManager.open(it)
                })
            }
        }
    }
}

@Composable
fun mangaPreview(model: Manga) {
    Column(modifier = Modifier.clickable {
        reader.viewersManager.open(
            model
        )
    }) {
        if (model.bannerUrl.isNotBlank()) {
            val bytes = reader.getImage(model.bannerUrl)
            Image(bytes, model.name, Modifier.border(1.dp, Color.Blue).size(200.dp, 310.dp))
        }

        model.name.let { if (it.length >= 25) "${it.substring(0, 22)}..." else it }
            .intoTextComponent()
    }
}

class ViewersManager {
    private val selection = SingleSelection()

    private val picker = MainPage()

    var viewers = mutableStateListOf<Viewer>()
        private set

    val active: Viewer? get() = selection.selected as Viewer?

    init {
        picker.selection = selection
        viewers.add(picker)
        picker.activate()
    }

    fun open(manga: Manga) {
        val viewer = MangaViewer(manga)

        val existingViewer =
            viewers.filterIsInstance<MangaViewer>().find { it.manga.uuid.toString() == manga.uuid.toString() }

        if (existingViewer != null) {
            return
        }

        viewer.selection = selection
        viewer.close = { close(viewer) }
        viewers.add(viewer)
        viewer.activate()
    }

    fun open(chapter: Chapter) {
        val viewer = ChapterViewer(chapter)

        val exisitingViewer = viewers.filterIsInstance<ChapterViewer>().find { it.chapter.uuid == viewer.chapter.uuid }

        if (exisitingViewer != null) {
            return
        }

        viewer.selection = selection
        viewer.close = { close(viewer) }
        viewers.add(viewer)
        viewer.activate()
    }

    fun open(viewer: Viewer) {
        viewer.selection = selection
        if (viewer is CloseableViewer) viewer.close = { close(viewer) }
        viewers.add(viewer)
        viewer.activate()
    }

    private fun close(viewer: Viewer) {
        val index = viewers.indexOf(viewer)
        viewers.remove(viewer)
        if (viewer.isActive) {
            selection.selected = viewers.getOrNull(index.coerceAtMost(viewers.lastIndex))
        }
    }

    fun update(oldView: Viewer, newView: Viewer) {
        val index = viewers.indexOf(oldView)
        viewers.remove(oldView)
        newView.selection = selection
        if (newView is CloseableViewer) newView.close = { close(newView) }
        viewers.add(index, newView)
        newView.activate()
    }
}

class MainPage : Viewer(ViewerState.PICKER) {
    override val title: String
        get() = "Main page"

    @Composable
    override fun toView() = MangaBrowser()
}

class SearchPage(val query: String) : CloseableViewer(ViewerState.SEARCH) {
    override val title: String
        get() = "Search results"

    @Composable
    override fun toView() = SearchResult(this)
}

class MangaViewer(val manga: Manga) : CloseableViewer(ViewerState.MANGA) {
    override val title: String
        get() = manga.name

    @Composable
    override fun toView() = MangaInfo(this)
}

class ChapterViewer(val chapter: Chapter) : CloseableViewer(ViewerState.READER) {
    override val title: String
        get() = "${chapter.manga.name} - ${chapter.name}"

    @Composable
    override fun toView() = ChapterInfo(this)
}

open class CloseableViewer(state: ViewerState) : Viewer(state) {
    var close: (() -> Unit)? = null
}

open class Viewer(open var state: ViewerState) {
    lateinit var selection: SingleSelection

    val isActive: Boolean
        get() = selection.selected === this

    open val title: String = ""

    @Suppress("UNNECESSARY_SAFE_CALL")
    fun activate() {
        selection.selected = this
        reader?.presenceClient?.updatePresence(
            "Read me a manga",
            if (title.length > 100) "${title.subSequence(0, 97)}..." else title
        )
    }

    @Composable
    open fun toView(): Unit = Unit
}

class SingleSelection {
    var selected: Any? by mutableStateOf(null)
}

class Settings {
    var mangaPerRow = 5
}

data class Manga(
    val name: String,
    val bannerUrl: String,
    val chapterList: MutableList<Chapter>,
    val infoPage: String,
    val provider: Scrapper,
    val uuid: UUID,
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
        setProperty("author", author)
        setProperty("description", description)
        setProperty("genres", genres.joinToString("|"))
        setProperty("status", status)
    }

    fun getDir() = File(reader.defaultMangaDir, uuid.toString())

    companion object {
        fun createFromProperties(props: Properties) = Manga(
            name = props.getProperty("name"),
            bannerUrl = props.getProperty("bannerUrl"),
            chapterList = mutableListOf(),
            infoPage = props.getProperty("infoPage"),
            provider = MangaReader.providers.find { it.url == props.getProperty("source") }!!,
            uuid = UUID.fromString(props.getProperty("uuid")),
            alternativeNames = props.getProperty("alternativeNames").split("|"),
            author = props.getProperty("author"),
            description = props.getProperty("description"),
            genres = props.getProperty("genres").split("|"),
            status = props.getProperty("status")
        )
    }
}

data class Chapter(
    val name: String,
    val chapterNumber: Double,
    val manga: Manga,
    val panes: MutableList<Pane>,
    val uuid: UUID,
    val infoPage: String
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
    }

    fun getDir() = File(manga.getDir(), uuid.toString())

    companion object {
        fun createFromProperties(props: Properties, manga: Manga) = Chapter(
            name = props.getProperty("name"),
            chapterNumber = props.getProperty("number").toDouble(),
            manga = manga,
            panes = mutableListOf(),
            uuid = UUID.fromString(props.getProperty("uuid")),
            infoPage = props.getProperty("infoPage")
        ).apply {
            panes.addAll(
                props.getProperty("panes").split("|").mapNotNull { if (it.isEmpty()) null else Pane(it, this) })
        }
    }
}

data class Pane(val url: String, val chapter: Chapter)
enum class ViewerState { PICKER, SEARCH, MANGA, READER }
enum class BorderSide { LEFT, RIGHT, TOP, BOTTOM }