package com.lipapp.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lipapp.data.model.Message
import com.lipapp.ui.theme.*
import com.lipapp.util.parseIrcFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val URL_REGEX = Regex(
    """https?://[^\s<>"{}|\\^`\[\]]+""",
    RegexOption.IGNORE_CASE,
)

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

@Composable
fun MessageList(
    messages: List<Message>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    isLoading: Boolean,
    myNick: String?,
    darkMode: Boolean,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val listItems = remember(messages) {
        buildList {
            var lastDate: String? = null
            for (msg in messages) {
                val msgDate = parseDate(msg.time)
                if (msgDate != null && msgDate != lastDate) {
                    lastDate = msgDate
                    add(ListItem.Date(msgDate))
                }
                add(ListItem.Msg(msg))
            }
        }.asReversed()
    }

    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex <= 2
        }
    }

    var lastSeenId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(messages.lastOrNull()?.id, listItems.size) {
        val newId = messages.lastOrNull()?.id
        if (newId != null && newId != lastSeenId && listItems.isNotEmpty()) {
            if (lastSeenId == null || isAtBottom) {
                listState.animateScrollToItem(0)
            }
            lastSeenId = newId
        }
    }

    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && listItems.isNotEmpty() && isAtBottom) {
            listState.animateScrollToItem(0)
        }
    }

    val lastVisibleIndex by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }
    }
    LaunchedEffect(lastVisibleIndex) {
        if (lastVisibleIndex >= listItems.size - 3 && hasMore && !isLoadingMore) {
            onLoadMore()
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            items(listItems, key = {
                when (it) {
                    is ListItem.Date -> "date_${it.label}"
                    is ListItem.Msg -> it.message.id
                }
            }) { item ->
                when (item) {
                    is ListItem.Date -> DateSeparator(item.label)
                    is ListItem.Msg -> MessageRow(message = item.message, myNick = myNick, darkMode = darkMode)
                }
            }

            if (isLoadingMore) {
                item(key = "_loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }

        if (!isAtBottom && listItems.size > 5) {
            SmallFloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Jump to latest")
            }
        }
    }
}

@Composable
private fun DateSeparator(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "— $date —",
            style = MaterialTheme.typography.bodySmall,
            color = MetaGray,
        )
    }
}

@Composable
private fun MessageRow(message: Message, myNick: String?, darkMode: Boolean) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val timestamp = formatTime(message.time)
    val linkColor = if (darkMode) IrcBlueDark else IrcBlue
    val mentionBg = if (darkMode) MentionDark else MentionLight

    val annotated = remember(message.id, darkMode) {
        buildMessageText(message, timestamp, myNick, linkColor, mentionBg)
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
    )
}

private fun buildMessageText(
    message: Message,
    timestamp: String,
    myNick: String?,
    linkColor: Color,
    mentionBg: Color,
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = TimestampGray)) {
        append("[$timestamp] ")
    }

    when (message.type) {
        "action" -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append("* ${message.from} ")
                appendFormatted(message.text, myNick, linkColor, mentionBg)
            }
        }
        "notice" -> {
            withStyle(SpanStyle(color = TimestampGray)) {
                append("-${message.from}- ")
            }
            appendFormatted(message.text, myNick, linkColor, mentionBg)
        }
        "meta" -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = MetaGray)) {
                append(message.text)
            }
        }
        else -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("<${message.from}> ")
            }
            appendFormatted(message.text, myNick, linkColor, mentionBg)
        }
    }
}

private fun AnnotatedString.Builder.appendFormatted(
    text: String,
    myNick: String?,
    linkColor: Color,
    mentionBg: Color,
) {
    val parsed = parseIrcFormat(text)
    val fullText = parsed.text

    val urls = URL_REGEX.findAll(fullText).toList()

    if (urls.isEmpty() && (myNick == null || !fullText.contains(myNick, ignoreCase = true))) {
        append(parsed)
        return
    }

    var pos = 0
    for (i in fullText.indices) {
        if (i < pos) continue

        val url = urls.find { i in it.range }
        if (url != null) {
            if (pos < url.range.first) {
                appendSegment(parsed, pos, url.range.first, myNick, mentionBg)
            }
            pushLink(LinkAnnotation.Url(
                url.value,
                TextLinkStyles(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
            ))
            append(url.value)
            pop()
            pos = url.range.last + 1
            continue
        }

        if (myNick != null && fullText.regionMatches(i, myNick, 0, myNick.length, ignoreCase = true)) {
            if (pos < i) {
                appendWithSpans(parsed, pos, i)
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, background = mentionBg)) {
                append(fullText.substring(i, i + myNick.length))
            }
            pos = i + myNick.length
            continue
        }
    }

    if (pos < fullText.length) {
        appendSegment(parsed, pos, fullText.length, myNick, mentionBg)
    }
}

private fun AnnotatedString.Builder.appendSegment(
    source: AnnotatedString,
    start: Int,
    end: Int,
    myNick: String?,
    mentionBg: Color,
) {
    if (myNick == null) {
        appendWithSpans(source, start, end)
        return
    }

    var pos = start
    val text = source.text
    while (pos < end) {
        val idx = text.indexOf(myNick, pos, ignoreCase = true)
        if (idx < 0 || idx >= end) {
            appendWithSpans(source, pos, end)
            break
        }
        if (idx > pos) appendWithSpans(source, pos, idx)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, background = mentionBg)) {
            append(text.substring(idx, idx + myNick.length))
        }
        pos = idx + myNick.length
    }
}

private fun AnnotatedString.Builder.appendWithSpans(
    source: AnnotatedString,
    start: Int,
    end: Int,
) {
    append(source.subSequence(start, end))
}

private sealed class ListItem {
    data class Date(val label: String) : ListItem()
    data class Msg(val message: Message) : ListItem()
}

private fun formatTime(isoTime: String): String {
    return try {
        ZonedDateTime.parse(isoTime)
            .withZoneSameInstant(ZoneId.systemDefault())
            .format(timeFormatter)
    } catch (_: DateTimeParseException) {
        try {
            isoTime.substring(11, 16)
        } catch (_: Exception) { "" }
    }
}

private fun parseDate(isoTime: String): String? {
    return try {
        ZonedDateTime.parse(isoTime)
            .withZoneSameInstant(ZoneId.systemDefault())
            .format(dateFormatter)
    } catch (_: Exception) { null }
}
