package views

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import reader
import scrappers.Chapter
import scrappers.UserStatus
import java.io.File
import java.net.URL

class ChapterViewer(var chapter: Chapter) : CloseableViewer(ViewerState.READER) {
    override val title: String
        get() = "${chapter.manga.name} - ${chapter.name}"

    private val referer: String
        get() = URL(chapter.infoPage).let { "${it.protocol}://${it.host}" }

    @Composable
    override fun toView() = Row(Modifier.fillMaxWidth()) {
        if (chapter.panes.isEmpty()) chapter = chapter.manga.provider.getChapter(chapter)

        if (statuses.size == 0) {
            statuses.add(TextStatus("Progress", "${chapter.lastPage + 1} / ${chapter.panes.size}"))
        }

        if (chapter.userStatus == UserStatus.NONE) {
            chapter.userStatus = UserStatus.READING
            updateProgress()
        }

        if (chapter.lastPage == chapter.panes.size) {
            chapter.lastPage = 0
            chapter.userStatus = UserStatus.DONE
            updateProgress()
        }

        createColumn(true)

        createImage()

        createColumn(false)
    }

    @Composable
    fun createColumn(left: Boolean = true) = Column(Modifier.fillMaxHeight()) {
        Icon(
            Icons.Default.let { if (left) it.KeyboardArrowLeft else it.KeyboardArrowRight },
            tint = LocalContentColor.current,
            contentDescription = if (left) "Left" else "Right",
            modifier = Modifier.padding(4.dp).clickable { if (left) moveLeft() else moveRight() }
                .fillMaxWidth(0.05f)
        )

        Icon(
            Icons.Default.let { if (left) it.ArrowBack else it.ArrowForward },
            tint = LocalContentColor.current,
            contentDescription = if (left) "Earlier chapter" else "Next chapter",
            modifier = Modifier.padding(4.dp).clickable { if (left) moveHardLeft() else moveHardRight() }
                .fillMaxWidth(0.05f)
        )
    }

    private fun moveLeft() {
        chapter.lastPage = (chapter.lastPage - 1).coerceAtLeast(0)
        updateProgress()
    }

    private fun moveHardLeft() {
        val list = chapter.manga.chapterList
        val index = list.indexOf(chapter)

        //List is in reversed order
        if (index == -1 || index + 1 >= list.size) return

        val earlierChapter = list[index + 1]
        reader.viewersManager.update(this, ChapterViewer(earlierChapter))
    }

    private fun moveRight() {
        if (chapter.lastPage < (chapter.panes.size - 1)) {
            chapter.lastPage = (chapter.lastPage + 1).coerceAtMost(chapter.panes.size - 1)
            updateProgress()
        } else {
            if (chapter.lastPage == (chapter.panes.size - 1)) {
                chapter.userStatus = UserStatus.DONE
                reader.addChapter(chapter.getDir(), chapter)
            }
        }
    }

    private fun moveHardRight() {
        val list = chapter.manga.chapterList
        //List is in reversed order
        val index = list.indexOf(chapter)

        if (index == -1 || list.size - index <= 0) return

        val earlierChapter = list[index - 1]
        reader.viewersManager.update(this, ChapterViewer(earlierChapter))
    }

    private fun updateProgress() {
        val chapterIndex = chapter.manga.chapterList.indexOfFirst { it.uuid == chapter.uuid }
        chapter.manga.chapterList[chapterIndex] = chapter
        reader.updateChapter(chapter.getDir(), chapter)
        statuses.removeLast()
        statuses.add(TextStatus("Progress", "${chapter.lastPage + 1} / ${chapter.panes.size}"))
    }

    @Composable
    private fun RowScope.createImage() {
        val image = getPaneBitMap(chapter.lastPage)

        val mods = Modifier.fillMaxHeight().weight(1f)

        Column(mods) {
            Image(image, chapter.name, Modifier.fillMaxWidth())
        }
    }

    private fun getPaneBitMap(index: Int): ImageBitmap = reader.getImage(
        chapter.panes[index].url,
        referer = referer,
        File(chapter.getDir(), "${index}.${chapter.panes[index].url.split(".").last()}")
    )
}