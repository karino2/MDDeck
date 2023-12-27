package io.github.karino2.mddeck.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wakaztahir.codeeditor.model.CodeLang
import com.wakaztahir.codeeditor.prettify.PrettifyParser
import com.wakaztahir.codeeditor.theme.CodeThemeType
import com.wakaztahir.codeeditor.utils.parseCodeAsAnnotatedString
import io.github.karino2.mddeck.MdCell
import io.github.karino2.mddeck.MDDeckVM
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Link
import org.commonmark.node.Heading as CHeading
import org.commonmark.node.Text as CText
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.ThematicBreak
import kotlin.math.roundToInt



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MDDecks(
    viewModel: MDDeckVM
) {
    // https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary
    // val toolbarHeight = 48.dp
    val toolbarHeight = 56.dp
    val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.roundToPx().toFloat() }
    val toolbarOffsetHeightPx = remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = toolbarOffsetHeightPx.value + delta
                toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    // val cscope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(modifier = Modifier
        .fillMaxSize()
        .nestedScroll(nestedScrollConnection)
        .background(Color(0xFF076D20))) {
        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier
                .verticalScroll(scrollState)
                .padding(top = toolbarHeight + 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                viewModel.blocks.value.forEachIndexed { index, block ->
                    key(block.dt) {
                        val node = viewModel.parse(block.src)
                        Markdown(block,
                            { viewModel.parse(it) },
                            onSelect = { newSelect ->
                                /*
                                viewModel.updateSelectionState(index, newSelect)
                                textState = viewModel.selectedBlock.value.src

                                 */
                            }
                        )
                        Spacer(modifier = Modifier.size(5.dp))
                    }
                }
            }

            TopAppBar(
                modifier = Modifier
                    .height(toolbarHeight)
                    .offset { IntOffset(x = 0, y = toolbarOffsetHeightPx.value.roundToInt()) },
                title = {
                    Text("MDDeck")
                }
            )

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
    onSelect: (isSelect: Boolean) -> Unit
) {
    val node = parseFun(mdCell.src)

    // draw bounding box and call onSelect
    val boxModifier = Modifier.clickable { onSelect(true) }
    Box(modifier = boxModifier
        .fillMaxWidth()
        .background(Color(0xFFFFFBFE))
        .padding(5.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            node.children.forEach {
                MdBlock(it)
            }
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
    val codeParser = remember { PrettifyParser() } // try getting from LocalPrettifyParser.current
    val themeState by remember { mutableStateOf(CodeThemeType.Monokai) }
    val theme = remember(themeState) { themeState.theme }

    val parsedCode = remember {
        parseCodeAsAnnotatedString(
            parser = codeParser,
            theme = theme,
            lang = lang2Enum(lang),
            code = code.trimEnd('\n')
        )
    }

    Text(parsedCode, Modifier.background(Color(46, 46, 46)).padding(5.dp))
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(
                    modifier = Modifier
                        .size(10.dp)
                        .padding(end = 5.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
