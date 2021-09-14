import androidx.compose.desktop.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import scrappers.Manga
import scrappers.Manganato
import scrappers.UserStatus
import views.CloseableViewer
import views.MangaReader
import views.Viewer
import views.ViewerState
import java.io.File
import java.net.URL

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
                    Box(Modifier.padding(10.dp)) { mangaPreview(manga).also { reader.addManga(manga) } }
                }
            }
        }
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
                Box(Modifier.padding(10.dp)) { mangaPreview(manga).also { reader.addManga(manga) } }
            }
        }
    }
}

@Composable
fun MangaInfo(model: MangaViewer) = Row {

    val manga = model.manga.provider.getInfo(URL(model.manga.infoPage), model.manga.preview, model.manga.uuid)

    Column(Modifier.fillMaxWidth(0.25f).padding(horizontal = 10.dp)) {

        if (manga.bannerUrl.isNotBlank()) {
            val bytes = reader.getImage(manga.bannerUrl)
            Image(bytes, manga.name, Modifier.border(1.dp, Color.Blue).size(200.dp, 310.dp))
        }

        manga.name.intoTextComponent(Modifier.padding(vertical = 5.dp))

        Row {

            Icon(
                if (reader.library.contains(manga.uuid)) Icons.Default.Delete
                else Icons.Default.Add,
                tint = LocalContentColor.current,
                contentDescription = "Add to library",
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp)
                    .clickable {
                        if (reader.library.contains(manga.uuid)) reader.removeFromLibrary(manga)
                        else reader.addToLibrary(manga)
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
                        val newManga = manga.provider.getInfo(URL(manga.infoPage), true)
                        reader.updateManga(newManga)
                        reader.viewersManager.update(model, MangaViewer(newManga))
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
                Surface(
                    color = when (it.userStatus) {
                        UserStatus.DONE -> Color.LightGray
                        UserStatus.READING -> Color.Cyan
                        UserStatus.NONE -> Color.Transparent
                    }
                ) {
                    Row {
                        it.name.intoTextComponent(Modifier.padding(10.dp).clickable {
                            it.userStatus = UserStatus.READING
                            reader.viewersManager.open(it)
                        })

                        if (it.userStatus == UserStatus.READING) {
                            "(${it.lastPage + 1})".intoTextComponent(Modifier.padding(10.dp))
                        }
                    }
                }
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
            val bytes = reader.getImage(
                model.bannerUrl,
                file = File(reader.defaultImageDir, "${model.uuid}.${model.bannerUrl.split(".").last()}")
            )
            Image(bytes, model.name, Modifier.border(1.dp, Color.Blue).size(200.dp, 310.dp))
        }

        model.name.let { if (it.length >= 25) "${it.substring(0, 22)}..." else it }
            .intoTextComponent()
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