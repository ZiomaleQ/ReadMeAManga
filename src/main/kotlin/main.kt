import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import scrappers.Manga
import scrappers.Manganato
import scrappers.UserStatus
import views.CloseableViewer
import views.MangaReader
import views.Viewer
import views.ViewerState
import java.net.URL

val reader = MangaReader()

fun main() = singleWindowApplication(
    title = "Read Me A Manga"
) {
    MaterialTheme { reader.createView() }
}

@Composable
fun MangaBrowser() {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        if (reader.library.isNotEmpty()) {
            "Library:".intoTextComponent(Modifier.align(alignment = Alignment.CenterHorizontally))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                reader.library.take(reader.settings.mangaPerRow * 2).map { reader.getManga(it)!! }.forEach {
                    Box(Modifier.padding(10.dp)) {
                        it.createPreview()
                    }
                }
            }
        } else "Library is empty... Add some manga on manga view!".intoTextComponent()
    }
}

@Composable
fun SearchResult(model: SearchPage) = Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
    val results = Manganato().search(model.query)

    for (i in 0..(Math.floorDiv(results.size, reader.settings.mangaPerRow))) {
        Row {
            for (manga in results.subList(
                i * reader.settings.mangaPerRow,
                (((i + 1) * reader.settings.mangaPerRow)).coerceAtMost(results.size)
            )) {
                Box(Modifier.padding(10.dp)) { manga.createPreview().also { reader.addManga(manga) } }
            }
        }
    }
}

@Composable
fun MangaInfo(model: MangaViewer) = Row {

    val manga = reader.resolveManga(model.manga)

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
                Row {
                    Box(Modifier.padding(10.dp).clickable { reader.viewersManager.open(it) }) {
                        val modif = when (it.userStatus) {
                            UserStatus.DONE -> Modifier.background(Color(0x808080))
                            UserStatus.READING -> Modifier.background(Color.LightGray)
                            else -> Modifier
                        }

                        val title = if (it.lastPage > 0) it.name.let { name -> "$name @${it.lastPage}" } else it.name
                        title.intoTextComponent(modif.padding(5.dp))
                    }
                }
            }
        }
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
        get() = "Search results - $query"

    @Composable
    override fun toView() = SearchResult(this)
}


class MangaViewer(var manga: Manga) : CloseableViewer(ViewerState.MANGA) {
    override val title: String
        get() = manga.name

    @Composable
    override fun toView() = MangaInfo(this)
}