package views

import BorderSide
import DiscordPresence
import FileKind
import SearchPage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import border
import createIfNot
import intoTextComponent
import scrappers.Chapter
import scrappers.Manga
import scrappers.Manganato
import scrappers.Scrapper
import java.io.File
import java.net.URL
import java.util.*

class MangaReader(
    val viewersManager: ViewersManager = ViewersManager(),
    var presenceClient: DiscordPresence = DiscordPresence()
) {
    val imageCache = HashMap<String, ImageBitmap>()
    val mangaCache = HashMap<UUID, Manga>()
    val defaultDir = File(System.getProperty("user.home"), ".rmam")
    val defaultImageDir = File(defaultDir, "images")
    val defaultMangaDir = File(defaultDir, "manga")
    val libraryFile = File(defaultDir, "library.props")
    val settings: Settings = Settings()

    val lastRead = mutableStateListOf<UUID>()
    val library = mutableStateListOf<UUID>()

    init {
        if (!defaultDir.exists()) defaultDir.mkdir()
        if (!defaultImageDir.exists()) defaultImageDir.mkdir()
        if (!defaultMangaDir.exists()) defaultMangaDir.mkdir()

        libraryFile.createIfNot(FileKind.FILE)
        val props = libraryFile.inputStream().use { Properties().apply { load(it) } }

        settings.init(this)

        for (manga in defaultMangaDir.listFiles()!!) loadManga(manga)
        lastRead.addAll(
            (props.getOrDefault("lastRead", "") as String)
                .split("|")
                .mapNotNull { if (it.isNotEmpty()) UUID.fromString(it) else null }
        )
        library.addAll(
            (props.getOrDefault("library", "") as String)
                .split("|")
                .mapNotNull { if (it.isNotEmpty()) UUID.fromString(it) else null }
        )
    }

    @Composable
    fun createView() = Box {
        Column {

            //Toolbar
            Row(Modifier.fillMaxWidth().height(40.dp).padding(4.dp).background(settings.theme.primaryVariant), horizontalArrangement = Arrangement.SpaceBetween) {
                "".intoTextComponent()
                Row {
                    Text("Looking for something?")
                    var searchText by remember { mutableStateOf("") }
                    BasicTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        singleLine = true,
                        modifier = Modifier.padding(4.dp).size(200.dp, 30.dp).border(2f, BorderSide.BOTTOM)
                    )
                    Icon(
                        Icons.Default.Search,
                        tint = LocalContentColor.current,
                        contentDescription = "Search",
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp)
                            .clickable {
                                viewersManager.open(SearchPage(searchText))
                            }
                    )
                }
                "".intoTextComponent()
            }

            viewersManager.createView()

            Box(Modifier.weight(1f)) {
                viewersManager.active!!.toView()
            }

            Box(Modifier.height(32.dp).fillMaxWidth().padding(4.dp)) {
                Row(Modifier.fillMaxHeight().align(Alignment.CenterStart)) {
                    for (status in viewersManager.active!!.statuses) createStatus(status)
                }
                Row(Modifier.fillMaxHeight().align(Alignment.CenterEnd)) {
                    createStatus(IconStatus("Discord", Icons.Default.Done))
                }
            }
        }
    }

    fun getImage(url: String, referer: String = "", file: File? = null): ImageBitmap {
        if (imageCache.containsKey(url)) return imageCache[url] ?: ImageBitmap(101, 141)

        if (url.isEmpty()) return ImageBitmap(101, 141)

        val imagePath = file ?: File(defaultImageDir, URL(url).path.replace("/", "_"))

        val bytes = if (!imagePath.exists()) {
            val con = if (referer.isEmpty()) {
                URL(url).openStream()
            } else {
                val connection = URL(url).openConnection()
                connection.setRequestProperty("Referer", referer)
                connection.connect()

                connection.getInputStream()
            }

            con.use { it.readAllBytes() }.also { imagePath.createNewFile(); imagePath.writeBytes(it) }
        } else {
            imagePath.readBytes()
        }

        val imageBitmap = org.jetbrains.skija.Image.makeFromEncoded(bytes).asImageBitmap()

        return imageBitmap.also { imageCache[url] = it }
    }

    fun addManga(manga: Manga, force: Boolean = false): Manga {
        val existingManga = mangaCache.values.find {
            it.provider.url == manga.provider.url && it.infoPage == manga.infoPage
        }
        if (!force && existingManga != null) return existingManga

        val id = manga.uuid.toString()
        val mangaDir = File(defaultMangaDir, id)
        mangaDir.createIfNot(FileKind.DIR)

        val mangaInfoFile = File(mangaDir, "info.props")
        val properties = manga.toProperties()

        mangaCache[manga.uuid] = manga

        val stream = mangaInfoFile.outputStream()
        stream.use { properties.store(it, "Manga info") }
        for (chapter in manga.chapterList) {
            addChapter(mangaDir, chapter)
        }
        return manga
    }

    fun updateManga(manga: Manga) {
        val oldManga = mangaCache.values.find {
            it.provider.url == manga.provider.url && it.infoPage == manga.infoPage
        } ?: return Unit.also { addManga(manga) }
        manga.uuid = oldManga.uuid

        val chapters = manga.chapterList.map { newChapter ->
            val oldChapter = oldManga.chapterList.find { it.infoPage == newChapter.infoPage }
            if (oldChapter == null) return@map newChapter.also { addChapter(oldManga.getDir(), newChapter) }
            else {
                newChapter.uuid = oldChapter.uuid

                val stream = newChapter.getInfo().outputStream()
                stream.use { newChapter.toProperties().store(it, "Chapter info") }

                newChapter
            }
        }

        manga.chapterList.clear()
        manga.chapterList.addAll(chapters)

        mangaCache[manga.uuid] = manga

        val stream = oldManga.getInfo().outputStream()
        stream.use { manga.toProperties().store(it, "Manga info") }
    }

    fun loadManga(file: File) {
        val mangaInfoFile = File(file, "info.props")
        if (!mangaInfoFile.exists()) {
            println("Removing useless cache [UUID: ${mangaInfoFile.parent}]")
            file.deleteRecursively()
        }

        val stream = mangaInfoFile.inputStream()
        val properties = Properties()
        stream.use { properties.load(it) }

        val manga = Manga.createFromProperties(properties)
        val chapters = properties.getProperty("chapterList").split("|").filterNot { it.isEmpty() }
        for (chapter in chapters) {
            manga.chapterList.add(loadChapter(File(file, chapter), manga))
        }

        mangaCache[manga.uuid] = manga
    }

    fun loadChapter(file: File, manga: Manga): Chapter {
        val stream = File(file, "info.props").inputStream()
        val properties = Properties()
        stream.use { properties.load(it); }

        val chap = Chapter.createFromProperties(properties, manga)
        if (chap.panes.size == 0 && !chap.manga.preview) {
            chap.panes.addAll(chap.manga.provider.getChapter(chap).panes)
            addChapter(file, chap)
        }
        return chap
    }

    fun addChapter(dir: File, chapter: Chapter) {
        val chapterDir = if (dir.parentFile == defaultMangaDir) File(dir, chapter.uuid.toString()) else dir
        chapterDir.createIfNot(FileKind.DIR)
        val chapterInfo = File(chapterDir, "info.props")

        val stream = chapterInfo.outputStream()
        val properties = chapter.toProperties()
        stream.use { properties.store(it, "Chapter info") }
    }

    fun addToLibrary(manga: Manga) {
        val props = libraryFile.inputStream().use { Properties().apply { load(it) } }

        if (library.contains(manga.uuid)) return

        library.add(manga.uuid)

        props.setProperty("library", library.joinToString { it.toString() })
        props.store(libraryFile.outputStream(), "Library file")
    }

    fun removeFromLibrary(manga: Manga) {
        val props = libraryFile.inputStream().use { Properties().apply { load(it) } }

        if (!library.contains(manga.uuid)) return

        library.remove(manga.uuid)

        props.setProperty("library", library.joinToString { it.toString() })
        props.store(libraryFile.outputStream(), "Library file")
    }

    fun getManga(id: UUID): Manga? = mangaCache.get(id)
    fun getManga(id: String): Manga? = getManga(UUID.fromString(id))

    companion object {
        val providers: List<Scrapper> = listOf(Manganato())
    }
}

class Settings {
    var settingsFile = File("")
    val mangaPerRow = 5
    val theme = if (false) darkColors() else lightColors()

    fun init(mangaReader: MangaReader) {
        settingsFile = File(mangaReader.defaultDir, "settings.props")
        settingsFile.createIfNot(FileKind.FILE)
    }
}

@Composable
fun RowScope.createStatus(status: Status) = when (status) {
    is IconStatus -> {
        status.name.intoTextComponent(Modifier.align(Alignment.CenterVertically))
        Spacer(Modifier.width(8.dp))
        Icon(
            status.icon,
            tint = LocalContentColor.current,
            contentDescription = "${status.name} description",
            modifier = Modifier.size(24.dp).padding(4.dp)
        )
    }
    is TextStatus -> {
        status.name.intoTextComponent(Modifier.align(Alignment.CenterVertically))
        Spacer(Modifier.width(8.dp))
        status.text.intoTextComponent(Modifier.align(Alignment.CenterVertically))
    }
    else -> Unit
}

interface Status
data class IconStatus(var name: String, var icon: ImageVector) : Status
data class TextStatus(var name: String, var text: String) : Status