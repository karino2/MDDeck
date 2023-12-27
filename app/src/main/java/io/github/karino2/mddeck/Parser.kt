package io.github.karino2.mddeck

import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.Node
import org.commonmark.parser.Parser

class Parser {
    private val parser by lazy {
        Parser.builder()
            .extensions(listOf(TablesExtension.create(), TaskListItemsExtension.create(), StrikethroughExtension.create()))
            .build()
    }

    fun parse(md: String) : Node {
        return parser.parse(md)
    }

}