package io.github.karino2.mddeck

import androidx.compose.runtime.mutableStateOf


class MDDeckVM {
    private val parser = Parser()

    val blocks = mutableStateOf(emptyList<MdCell>())

    fun appendCell(md: MdCell) {
        blocks.value = blocks.value.appendHead(md)
    }

    fun updateCell(md: MdCell) {
        blocks.value = blocks.value.update(md)
    }

    fun parse(src: String) = parser.parse(src)

    var notifyCellClicked : (MdCell)->Unit = {}
    var notifyNewCell : ()->Unit = {}
    var notifyRefresh : ()->Unit = {}
    var notifySettings : ()->Unit = {}
}
