import androidx.compose.desktop.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import scrappers.Manganato
import scrappers.Scrapper
import java.net.URL
import java.util.*

class MangaReader(
    val viewersManager: ViewersManager = ViewersManager(),
    val settings: Settings = Settings(),
    var presenceClient: DiscordPresence = DiscordPresence()
)

val reader = MangaReader()

fun main() = Window(
    title = "Read Me A Manga",
    size = IntSize(1280, 768)
) {
    MaterialTheme { MangaReaderView(reader) }
}

@Composable
fun MangaReaderView(model: MangaReader) {
    Box {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                model.viewersManager.viewers.forEach { ViewersTabView(it) }
            }

            Box(Modifier.weight(1f)) {
                model.viewersManager.active!!.toView()
            }

            Box(Modifier.height(32.dp).fillMaxWidth().padding(4.dp)) {
                Row(Modifier.fillMaxHeight().align(Alignment.CenterEnd)) {
                    createStatus("Discord", Icons.Default.Done, "It's okay!")
                }
            }
        }
    }
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
                modifier = Modifier.size(100.dp, 36.dp).padding(4.dp).drawBehind {
                    val strokeWidth = 2f
                    val y = size.height - strokeWidth

                    drawLine(
                        Color.Gray,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth
                    )
                }
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

    Icon(
        Icons.Default.KeyboardArrowLeft,
        tint = LocalContentColor.current,
        contentDescription = "Left",
        modifier = Modifier.padding(4.dp).clickable {
            if (pageNumber > 0) pageNumber -= 1
        }.fillMaxWidth(0.05f)
    )

    val referer = URL(chapter.infoPage).let { "${it.protocol}://${it.host}" }

    val bytes = loadImageUrl(chapter.panes[pageNumber].url, referer = referer)
    Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).weight(1f)) {
        Image(bytes, chapter.name, Modifier.border(1.dp, Color.Blue).fillMaxWidth())
    }

    Icon(
        Icons.Default.KeyboardArrowRight,
        tint = LocalContentColor.current,
        contentDescription = "Right",
        modifier = Modifier.padding(4.dp).clickable {
            if (pageNumber < (chapter.panes.size - 1)) pageNumber += 1
        }.fillMaxWidth(0.05f)
    )

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
            val bytes = loadImageUrl(manga.bannerUrl)
            Image(bytes, manga.name, Modifier.border(1.dp, Color.Blue).size(200.dp, 310.dp))
        }

        manga.name.intoTextComponent(Modifier.padding(vertical = 5.dp))

        Box(Modifier.padding(vertical = 10.dp)) {
            manga.description?.intoTextComponent() ?: "No description".intoTextComponent()
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
            val bytes = loadImageUrl(model.bannerUrl)
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

        val exisitingViewer =
            viewers.filterIsInstance<MangaViewer>().find { it.manga.uuid.toString() == manga.uuid.toString() }

        if (exisitingViewer != null) {
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
}

class MainPage : Viewer(ViewerState.PICKER) {
    override val title: String
        get() = "Main page"

    @Composable
    override fun toView() {
        MangaBrowser()
    }
}

class SearchPage(val query: String) : CloseableViewer(ViewerState.SEARCH) {
    override val title: String
        get() = "Search results"

    @Composable
    override fun toView() {
        SearchResult(this)
    }
}

class MangaViewer(val manga: Manga) : CloseableViewer(ViewerState.MANGA) {
    override val title: String
        get() = manga.name

    @Composable
    override fun toView() {
        MangaInfo(this)
    }
}

class ChapterViewer(val chapter: Chapter) : CloseableViewer(ViewerState.READER) {
    override val title: String
        get() = "${chapter.manga.name} - ${chapter.name}"

    @Composable
    override fun toView() {
        ChapterInfo(this)
    }
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

@Composable
fun RowScope.createStatus(name: String, icon: ImageVector, description: String) {
    name.intoTextComponent(Modifier.align(Alignment.CenterVertically))
    Spacer(Modifier.width(8.dp))
    Icon(
        icon,
        tint = LocalContentColor.current,
        contentDescription = description,
        modifier = Modifier.size(24.dp).padding(4.dp)
    )
}

val imageCache = HashMap<String, ImageBitmap>()

fun loadImageUrl(url: String, referer: String = ""): ImageBitmap {
    if (imageCache.containsKey(url)) return imageCache[url] ?: ImageBitmap(101, 141)

    val con = if (referer.isEmpty()) {
        URL(url).openStream()
    } else {
        val connection = URL(url).openConnection()
        connection.setRequestProperty("Referer", referer)
        connection.connect()

        connection.getInputStream()
    }

    val bytes = con.use { it.readAllBytes() }
    val imageBitmap = org.jetbrains.skija.Image.makeFromEncoded(bytes).asImageBitmap()

    return imageBitmap.also { imageCache[url] = it }
}

@Composable
fun String.intoTextComponent(modifier: Modifier? = null) = Text(
    text = this,
    modifier = modifier ?: Modifier,
    fontSize = 12.sp
)

private operator fun TextUnit.minus(other: TextUnit) = (value - other.value).sp
private operator fun TextUnit.div(other: TextUnit) = value / other.value

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
    val status: String? = null
) {
    fun addChapter(name: String, number: Double, panes: MutableList<Pane>, infoPage: String) {
        chapterList.add(Chapter(name, number, this, panes, UUID.randomUUID(), infoPage))
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
}

data class Pane(val url: String, val chapter: Chapter)
enum class ViewerState { PICKER, SEARCH, MANGA, READER }