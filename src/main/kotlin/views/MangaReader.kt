package views

import Chapter
import DiscordPresence
import FileKind
import Manga
import Settings
import ViewersManager
import ViewersTabView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import createIfNot
import intoTextComponent
import scrappers.Manganato
import scrappers.Scrapper
import java.io.File
import java.net.URL
import java.util.*

class MangaReader(
    val viewersManager: ViewersManager = ViewersManager(),
    val settings: Settings = Settings(),
    var presenceClient: DiscordPresence = DiscordPresence()
) {
    val imageCache = HashMap<String, ImageBitmap>()
    val mangaCache = HashMap<UUID, Manga>()
    val defaultDir = File(System.getProperty("user.home"), ".rmam")
    val defaultImageDir = File(defaultDir, "images")
    val defaultMangaDir = File(defaultDir, "manga")

    init {
        if (!defaultDir.exists()) defaultDir.mkdir()
        if (!defaultImageDir.exists()) defaultImageDir.mkdir()
        if (!defaultMangaDir.exists()) defaultMangaDir.mkdir()

        for (manga in defaultMangaDir.listFiles()!!) loadManga(manga)
    }

    @Composable
    fun createView() {
        Box {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    viewersManager.viewers.forEach { ViewersTabView(it) }
                }

                Box(Modifier.weight(1f)) {
                    viewersManager.active!!.toView()
                }

                Box(Modifier.height(32.dp).fillMaxWidth().padding(4.dp)) {
                    Row(Modifier.fillMaxHeight().align(Alignment.CenterEnd)) {
                        createStatus("Discord", Icons.Default.Done, "It's okay!")
                    }
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

    fun addManga(manga: Manga): Manga {
        val existingManga = mangaCache.values.find {
            it.provider.url == manga.provider.url && it.infoPage == manga.infoPage
        }
        if (existingManga != null) return existingManga
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

        for (chapter in properties.getProperty("chapterList").split("|")) {
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

    fun getManga(id: UUID): Manga? = mangaCache.get(id)
    fun getManga(id: String): Manga? = getManga(UUID.fromString(id))

    companion object {
        val providers: List<Scrapper> = listOf(Manganato())
    }
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