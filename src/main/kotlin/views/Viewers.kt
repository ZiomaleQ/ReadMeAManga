package views

import Chapter
import ChapterViewer
import CloseableViewer
import MainPage
import Manga
import MangaViewer
import androidx.compose.runtime.*
import reader

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

enum class ViewerState { PICKER, SEARCH, MANGA, READER }