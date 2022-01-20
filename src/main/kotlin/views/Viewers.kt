package views

import MainPage
import MangaViewer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import intoTextComponent
import reader
import scrappers.Chapter
import scrappers.Manga

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

        val existingViewer = viewers.filterIsInstance<ChapterViewer>().find { it.chapter.uuid == viewer.chapter.uuid }

        if (existingViewer != null) {
            existingViewer.activate()
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

    @Composable
    fun createView() = Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        viewers.forEach {
            Surface(color = if (it.isActive) Color.LightGray else Color.Transparent) {

                Row(
                    Modifier
                        .clickable(remember { MutableInteractionSource() }, indication = null) { it.activate() }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    it.title.intoTextComponent(Modifier.padding(horizontal = 4.dp))

                    if ((it as? CloseableViewer)?.close != null) {
                        Icon(
                            Icons.Default.Close,
                            tint = LocalContentColor.current,
                            contentDescription = "Close",
                            modifier = Modifier.size(24.dp).padding(4.dp).clickable { it.close?.let { it() } }
                        )
                    } else {
                        Box(modifier = Modifier.size(24.dp).padding(4.dp))
                    }
                }

            }
        }
    }
}

open class Viewer(open var state: ViewerState) {
    lateinit var selection: SingleSelection

    val statuses = mutableStateListOf<Status>()

    val isActive: Boolean
        get() = selection.selected === this

    open val title: String = ""

    @Suppress("UNNECESSARY_SAFE_CALL")
    fun activate() {
        selection.selected = this
        reader?.presenceClient?.updatePresence(
            "ReadMeAManga",
            if (title.length > 100) "${title.subSequence(0, 97)}..." else title
        )
    }

    @Composable
    open fun toView(): Unit = Unit
}

class SingleSelection {
    var selected: Any? by mutableStateOf(null)
}

enum class ViewerState { PICKER, SEARCH, MANGA, READER }
open class CloseableViewer(state: ViewerState) : Viewer(state) {
    var close: (() -> Unit)? = null
}