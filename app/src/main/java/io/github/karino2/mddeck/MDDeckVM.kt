package io.github.karino2.mddeck

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel


class MDDeckVM : ViewModel() {
    private val parser = Parser()

    val blocks = mutableStateOf(emptyList<MdCell>())

    /*
    private fun onBlocksChange(newBlocks: List<Block>, notifySave : Boolean = true) {
    blocks.value = newBlocks
    selectedBlock.value = emptyBlock
    if(notifySave) {
        _notifySaveState.value = _notifySaveState.value?.let { it +1 } ?: 0
    }
    }
     */


    /*
    fun updateBlock(dt: Date, blockSrc: String) {
        val newBlocks = blocks.value.replace(dt, blockSrc)
        onBlocksChange(newBlocks)
    }

    fun appendTailBlocks(blockSrc: String) {
        if(blockSrc != "") {
            val newBlocks = blocks.value.appendTail(_splitter, blockSrc)
            onBlocksChange(newBlocks)
        }
    }

    fun updateSelectionState(idx: Int, isOpen: Boolean) {
        selectedBlock.value = if(isOpen) blocks.value[idx] else emptyBlock
    }

    fun openMd(newMd: String) {
        onBlocksChange(BlockList.toBlocks(parser.splitBlocks(newMd)), false)
    }

    fun parseBlock(src: String) = parser.parseBlock(src)
     */
    fun parse(src: String) = parser.parse(src)
}
