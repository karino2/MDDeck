package io.github.karino2.mddeck.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakaztahir.codeeditor.model.CodeLang
import com.wakaztahir.codeeditor.prettify.PrettifyParser
import com.wakaztahir.codeeditor.theme.CodeThemeType
import com.wakaztahir.codeeditor.utils.parseCodeAsAnnotatedString
import io.github.karino2.mddeck.MDDeckVM
import io.github.karino2.mddeck.MdCell
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.ThematicBreak
import java.text.DateFormat
import org.commonmark.node.Heading as CHeading
import org.commonmark.node.Text as CText


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MDDecks(
    viewModel: MDDeckVM
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                scrollBehavior = scrollBehavior,
                title = {
                    Text("MDDeck")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { viewModel.notifyRefresh() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { viewModel.notifySettings() }) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }

                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.notifyNewCell() }) {
                Icon(Icons.Filled.Add, "Floating action button for new cell.")
            }
        }
    ) { innerPadding->
        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFF076D20)),
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
                viewModel.blocks.value.forEach { block ->
                    key(block.dt) {
                        ElevatedCard(
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.notifyCellClicked(block) }
                            ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Markdown(block, { viewModel.parse(it) })
                                Text(DateFormat.getDateTimeInstance().format(block.dt), color=Color(150, 150, 150), modifier= Modifier.align(Alignment.End).padding(5.dp), )
                            }

                        }
                    }
                }
        }
    }
}

val Node.children : List<Node>
    get(){
        var child: Node? = this.firstChild ?: return emptyList()

        return sequence {
            while(child != null){
                yield(child!!)
                child = child!!.next
            }
        }.toList()
    }

@Composable
fun Markdown(
    mdCell: MdCell,
    parseFun: (block: String) -> Node,
) {
    val node = parseFun(mdCell.src)

    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(5.dp)) {
        node.children.forEach {
            MdBlock(it)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdBlocks(blocks: Node) {
    var prevCheck : TaskListItemMarker? = null
    blocks.children.forEach {
        // check box should be inline element, but it's the only exception. So I put here for that special handlijng.
        if (it is TaskListItemMarker)
        {
            prevCheck = it
        }
        else
        {
            val pc = prevCheck // for smart cast
            if (pc != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // This code does not offer touch handling, so no need to add extra margin.
                    // [android - Remove Default padding around checkboxes in Jetpack Compose new update - Stack Overflow](https://stackoverflow.com/questions/71609051/remove-default-padding-around-checkboxes-in-jetpack-compose-new-update)
                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        // "[ ] " or "[x] "
                        Checkbox(checked = pc.isChecked, {}) // , modifier = Modifier.absoluteOffset(0.dp, (-10).dp))
                    }
                    MdBlock(it)
                }
                prevCheck = null
            } else {
                MdBlock(it)
            }
        }
    }
    /*
    blocks.children.forEach {
        MdBlock(it)
    }
     */
}


@Composable
fun AnnotatedBox(
    content: AnnotatedString,
    paddingBottom: Dp,
    style: TextStyle = LocalTextStyle.current
) {
    Box(Modifier.padding(bottom = paddingBottom)) { Text(content, style = style) }
}


@Composable
fun Heading(block: CHeading, style: TextStyle) {
    AnnotatedBox(buildAnnotatedString {
        block.children.forEach {
            if (it is CText)
                append(it.literal)
            else
                println(it.toString())
        }
    }, 0.dp, style)
}

fun AnnotatedString.Builder.withStyle(
    style: SpanStyle,
    builder: AnnotatedString.Builder.() -> Unit
) {
    pushStyle(style)
    this.builder()
    pop()
}

fun AnnotatedString.Builder.appendChildrenInline(
    node: Node,
    colors: ColorScheme
) {
    val targets = node.children
    targets.forEachIndexed { index, child ->
        when(child) {
            is Code -> {
                // val bgcolor = Color(0xFFF5F5F5)
                val bgcolor = Color.LightGray
                withStyle(SpanStyle(color = Color.Red, background = bgcolor)) {
                    append(child.literal)
                }
            }
            is CText -> {
                append(child.literal)
            }
            is StrongEmphasis -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendChildrenInline(child, colors)
                }
            }
            is Emphasis -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendChildrenInline(child, colors)
                }

            }
            is HardLineBreak -> {
                append("\n")
            }
            is Strikethrough -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendChildrenInline(child, colors)
                }
            }
            is Link -> {
                withStyle(
                    SpanStyle(
                        colors.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    child.children.forEach {
                        if (it is CText)
                            append(it.literal)
                    }
                }
            }
            else -> {
                println(child.toString())
            }
        }
    }
}

@Composable
fun MdBlock(block: Node) {
    when(block) {
        is CHeading -> {
            when(block.level) {
                1 -> Heading(block, MaterialTheme.typography.headlineLarge)
                2 -> Heading(block, MaterialTheme.typography.headlineMedium)
                3 -> Heading(block, MaterialTheme.typography.headlineSmall)
                4 -> Heading(block, MaterialTheme.typography.titleLarge)
                5 -> Heading(block, MaterialTheme.typography.titleMedium)
                else -> Heading(block, MaterialTheme.typography.titleSmall)
            }
        }
        is FencedCodeBlock -> {
            CodeFence(block.info, block.literal)
        }
        is Paragraph -> {
            println(block.toString())
            AnnotatedBox(buildAnnotatedString {
                appendChildrenInline(block, MaterialTheme.colorScheme)
            }, 0.dp)

        }
        is OrderedList -> {
            MdOrderedList(block)
        }
        is BulletList -> {
            MdUnorderedList(block)
        }
        is ThematicBreak -> {
            // for click target.
            Box(modifier = Modifier.height(10.dp)) {
                Divider(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.DarkGray,
                    thickness = 2.dp
                )
            }
        }
        else -> {
            println(block.toString())
        }
    }
    // println(block.toStirng())
}

private fun lang2Enum(lang: String) : CodeLang {
    return when(lang) {
        "powershell" -> CodeLang.Default
        "kotlin" -> CodeLang.Kotlin
        "java" -> CodeLang.Java
        "cpp" -> CodeLang.CPP
        "html" -> CodeLang.HTML
        "c" -> CodeLang.C
        "csharp" -> CodeLang.CSharp
        "python" -> CodeLang.Python
        "javascript" -> CodeLang.JavaScript
        "bash" -> CodeLang.Bash
        "sh" -> CodeLang.Bash
        "fsharp" -> CodeLang.FSharp
        else -> CodeLang.Default
    }
}

@Composable
fun CodeFence(lang: String, code: String) {
    val codeParser = PrettifyParser() // try getting from LocalPrettifyParser.current
    val theme = CodeThemeType.Monokai.theme

    val langEnum = lang2Enum(lang)
    val content = code.trimEnd('\n')

    val mod = Modifier
        .background(Color(46, 46, 46))
        .padding(5.dp)

    if(langEnum == CodeLang.Default) {
        Text(content, mod, color = Color.White)
    } else {
        val parsedCode = parseCodeAsAnnotatedString(
            parser = codeParser,
            theme = theme,
            lang = langEnum,
            code = content
        )
        Text(parsedCode, mod)
    }

}

@Composable
inline fun MdListColumn(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .offset(x = 10.dp)
            .padding(0.dp)
    ) { content() }
}

@Composable
fun MdUnorderedList(list: BulletList) {
    MdListColumn {
        list.children.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Canvas(
                    modifier = Modifier
                        .size(10.dp)
                        .padding(end = 5.dp)
                        .offset(x = 0.dp, y = 10.dp)
                ) {
                    drawCircle(radius = size.width / 2, center = center, color = Color.Black)
                }
                Box {
                    Column {
                        MdBlocks(item)
                    }
                }
            }

        }

    }
}

@Composable
fun MdOrderedList(list: OrderedList) {
    val delim = list.delimiter
    val start = list.startNumber
    list.children.forEach {
        // it ListItem
        println(it.toString())
    }

    MdListColumn {
        val items = list.children
        val heads = start ..< (start+items.size)

        heads.zip(items)
            .forEach { (head, item) ->
                val mark = "${head}."
                Row(verticalAlignment = Alignment.Top) {
                    // I want to align
                    // 1. ...
                    // 2. ...
                    // I add 20.dp. This might not be enough, but I don't known what is the correct value.
                    Box(
                        Modifier
                            .padding(end = 5.dp)
                            .width(20.dp)) {
                        Text(mark)
                    }
                    Box {
                        Column {
                            // item : ListItem
                            MdBlocks(item)
                        }
                    }
                }
            }
    }
}
