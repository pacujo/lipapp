package com.lipapp.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
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

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleIndex) {
        if (firstVisibleIndex < 3 && hasMore && !isLoadingMore) {
            onLoadMore()
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        if (isLoadingMore) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }

        var lastDate: String? = null
        items(messages, key = { it.id }) { message ->
            val msgDate = parseDate(message.time)
            if (msgDate != null && msgDate != lastDate) {
                lastDate = msgDate
                DateSeparator(msgDate)
            }
            MessageRow(message = message, myNick = myNick, darkMode = darkMode)
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
    val uriHandler = LocalUriHandler.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val timestamp = formatTime(message.time)
    val linkColor = if (darkMode) IrcBlueDark else IrcBlue
    val mentionBg = if (darkMode) MentionDark else MentionLight

    val annotated = remember(message.id, darkMode) {
        buildMessageText(message, timestamp, myNick, linkColor, mentionBg)
    }

    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { uriHandler.openUri(it.item) }
        },
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
            pushStringAnnotation("URL", url.value)
            withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                append(url.value)
            }
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

private fun formatTime(isoTime: String): String {
    return try {
        ZonedDateTime.parse(isoTime).format(timeFormatter)
    } catch (_: DateTimeParseException) {
        try {
            isoTime.substring(11, 16)
        } catch (_: Exception) { "" }
    }
}

private fun parseDate(isoTime: String): String? {
    return try {
        ZonedDateTime.parse(isoTime).format(dateFormatter)
    } catch (_: Exception) { null }
}
