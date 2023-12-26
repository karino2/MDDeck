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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.karino2.mddeck.MdCell
import io.github.karino2.mddeck.MDDeckVM
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.ast.impl.ListCompositeNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import kotlin.math.roundToInt


// src is topLevelBlock.
data class RenderContext(val mdCell: MdCell) {
    val src: String
        get() = mdCell.src
}

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
                .padding(top = toolbarHeight+10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                viewModel.blocks.value.forEachIndexed { index, block ->
                    key(block.dt) {
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

@Composable
fun Markdown(
    mdCell: MdCell,
    parseFun: (block: String) -> ASTNode,
    onSelect: (isSelect: Boolean) -> Unit
) {
    val node = parseFun(mdCell.src)
    val ctx = RenderContext(mdCell)

    // draw bounding box and call onSelect
    val boxModifier = Modifier.clickable { onSelect(true) }
    Box(modifier = boxModifier.fillMaxWidth().background(Color(0xFFFFFBFE))) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            node.children.forEach{
                MdBlock(ctx, it, true)
            }

        }
    }
}

@Composable
fun MdBlocks(ctx: RenderContext, blocks: CompositeASTNode, isTopLevel: Boolean = false) {
    blocks.children.forEach { MdBlock(ctx, it, isTopLevel) }
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
fun Heading(ctx: RenderContext, block: CompositeASTNode, style: TextStyle) {
    AnnotatedBox(buildAnnotatedString {
        block.children.forEach { appendHeadingContent(ctx.src, it, MaterialTheme.colorScheme) }
    }, 0.dp, style)
}

fun AnnotatedString.Builder.appendHeadingContent(md: String, node: ASTNode, colors: ColorScheme) {
    when (node.type) {
        MarkdownTokenTypes.ATX_CONTENT -> {
            appendTrimmingInline(md, node, colors)
            return
        }
    }
    if (node is CompositeASTNode) {
        node.children.forEach { appendHeadingContent(md, it, colors) }
        return
    }
}

fun selectTrimmingInline(node: ASTNode): List<ASTNode> {
    val children = node.children
    var from = 0
    while (from < children.size && children[from].type == MarkdownTokenTypes.WHITE_SPACE) {
        from++
    }
    var to = children.size
    while (to > from && children[to - 1].type == MarkdownTokenTypes.WHITE_SPACE) {
        to--
    }

    return children.subList(from, to)
}


fun AnnotatedString.Builder.withStyle(
    style: SpanStyle,
    builder: AnnotatedString.Builder.() -> Unit
) {
    pushStyle(style)
    this.builder()
    pop()
}

fun AnnotatedString.Builder.appendInline(
    md: String,
    node: ASTNode,
    childrenSelector: (ASTNode) -> List<ASTNode>,
    colors: ColorScheme
) {
    val targets = childrenSelector(node)
    targets.forEachIndexed { index, child ->
        if (child is LeafASTNode) {
            when (child.type) {
                MarkdownTokenTypes.EOL -> {
                    // treat as space, except the case of BR EOL
                    if (index != 0 && targets[index - 1].type != MarkdownTokenTypes.HARD_LINE_BREAK)
                        append(" ")
                }
                MarkdownTokenTypes.HARD_LINE_BREAK -> append("\n")
                else -> append(child.getTextInNode(md).toString())
            }

        } else {
            when (child.type) {
                MarkdownElementTypes.CODE_SPAN -> {
                    // val bgcolor = Color(0xFFF5F5F5)
                    val bgcolor = Color.LightGray
                    pushStyle(SpanStyle(color = Color.Red, background = bgcolor))
                    child.children.subList(1, child.children.size - 1).forEach { item ->
                        append(item.getTextInNode(md).toString())
                    }
                    pop()
                }
                MarkdownElementTypes.STRONG -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInline(
                            md,
                            child,
                            { parent -> parent.children.subList(2, parent.children.size - 2) },
                            colors
                        )
                    }
                }
                MarkdownElementTypes.EMPH -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendInline(
                            md,
                            child,
                            { parent -> parent.children.subList(1, parent.children.size - 1) },
                            colors
                        )
                    }
                }
                MarkdownElementTypes.INLINE_LINK -> {
                    withStyle(
                        SpanStyle(
                            colors.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        child.children.filter { it.type == MarkdownElementTypes.LINK_TEXT }
                            .forEach { linktext ->
                                linktext.children.subList(1, linktext.children.size - 1).forEach {
                                    append(it.getTextInNode(md).toString())
                                }
                            }
                    }
                }
                GFMElementTypes.STRIKETHROUGH -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        appendInline(
                            md,
                            child,
                            { parent -> parent.children.subList(2, parent.children.size - 2) },
                            colors
                        )
                    }
                }
            }
        }
    }
}

fun AnnotatedString.Builder.appendTrimmingInline(md: String, node: ASTNode, colors: ColorScheme) {
    appendInline(md, node, ::selectTrimmingInline, colors)
}


@Composable
fun MdBlock(ctx: RenderContext, block: ASTNode, isTopLevel: Boolean) {
    when (block.type) {
        MarkdownElementTypes.ATX_1 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.headlineLarge)
        }
        MarkdownElementTypes.ATX_2 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.headlineMedium)
        }
        MarkdownElementTypes.ATX_3 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.headlineSmall)
        }
        MarkdownElementTypes.ATX_4 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.titleLarge)
        }
        MarkdownElementTypes.ATX_5 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.titleMedium)
        }
        MarkdownElementTypes.ATX_6 -> {
            Heading(ctx, block as CompositeASTNode, MaterialTheme.typography.titleSmall)
        }
        MarkdownElementTypes.PARAGRAPH -> {
            AnnotatedBox(buildAnnotatedString {
                appendTrimmingInline(ctx.src, block, MaterialTheme.colorScheme)
            }, if (isTopLevel) 8.dp else 0.dp)
        }
        MarkdownElementTypes.UNORDERED_LIST -> {
            MdUnorderedList(ctx, block as ListCompositeNode, isTopLevel)
        }
        MarkdownElementTypes.ORDERED_LIST -> {
            MdOrderedList(ctx, block as ListCompositeNode, isTopLevel)
        }

        MarkdownElementTypes.CODE_FENCE -> {
            CodeFence(ctx.src, block)
        }

        MarkdownTokenTypes.HORIZONTAL_RULE -> {
            // for click target.
            Box(modifier = Modifier.height(10.dp)) {
                Divider(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.DarkGray,
                    thickness = 2.dp
                )
            }
        }
    }
    // println(block.type.name)
}

// similar to CodeFenceGeneratingProvider at GeneratingProviders.kt
@Composable
fun CodeFence(md: String, node: ASTNode) {
    Column(
        modifier = Modifier
            .background(Color.LightGray)
            .padding(8.dp)
    ) {
        val codeStyle = TextStyle(fontFamily = FontFamily.Monospace)
        val builder = StringBuilder()

        var childrenToConsider = node.children
        if (childrenToConsider.last().type == MarkdownTokenTypes.CODE_FENCE_END) {
            childrenToConsider = childrenToConsider.subList(0, childrenToConsider.size - 1)
        }

        var lastChildWasContent = false

        var renderStart = false
        for (child in childrenToConsider) {
            if (!renderStart && child.type == MarkdownTokenTypes.EOL) {
                renderStart = true
            } else {
                when (child.type) {
                    MarkdownTokenTypes.CODE_FENCE_CONTENT -> {
                        builder.append(child.getTextInNode(md))
                        lastChildWasContent = true
                    }
                    MarkdownTokenTypes.EOL -> {
                        Text(
                            style = codeStyle,
                            text = builder.toString()
                        )
                        builder.clear()
                        lastChildWasContent = false
                    }
                }

            }
        }
        if (lastChildWasContent) {
            Text(
                style = codeStyle,
                text = builder.toString()
            )
        }

    }
}


@Composable
inline fun MdListColumn(
    isTopLevel: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .offset(x = if (isTopLevel) 5.dp else 10.dp)
            .padding(bottom = if (isTopLevel) 5.dp else 0.dp)
    ) { content() }
}

@Composable
fun MdUnorderedList(ctx: RenderContext, list: ListCompositeNode, isTopLevel: Boolean) {
    MdListColumn(isTopLevel) {
        list.children.forEach { item ->
            if (item.type == MarkdownElementTypes.LIST_ITEM) {
                Row {
                    Canvas(
                        modifier = Modifier
                            .size(10.dp)
                            .offset(y = 7.dp)
                            .padding(end = 5.dp)
                    ) {
                        drawCircle(radius = size.width / 2, center = center, color = Color.Black)
                    }
                    Box {
                        Column {
                            MdBlocks(ctx, item as CompositeASTNode)
                        }
                    }
                }
            }

        }

    }
}

@Composable
fun MdOrderedList(ctx: RenderContext, list: ListCompositeNode, isTopLevel: Boolean) {
    MdListColumn(isTopLevel) {
        val items = list.children
            .filter { it.type == MarkdownElementTypes.LIST_ITEM }

        val heads = items.runningFold(0) { aggr, item ->
            if (aggr == 0) {
                item.findChildOfType(MarkdownTokenTypes.LIST_NUMBER)
                    ?.getTextInNode(ctx.src)?.toString()?.trim()?.let {
                        val number = it.substring(0, it.length - 1).trimStart('0')
                        if (number.isEmpty()) 0 else number.toInt()
                    } ?: 1
            } else {
                aggr + 1
            }
        }.drop(1)

        heads.zip(items)
            .forEach { (head, item) ->
                val mark = "${head}."
                Row {
                    Box(Modifier.padding(end = 5.dp)) {
                        Text(mark)
                    }
                    Box {
                        Column {
                            MdBlocks(ctx, item as CompositeASTNode)
                        }
                    }
                }
            }
    }
}
