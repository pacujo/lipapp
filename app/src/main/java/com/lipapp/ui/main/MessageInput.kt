package com.lipapp.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.lipapp.util.IRC_BOLD
import com.lipapp.util.IRC_ITALIC
import com.lipapp.util.IRC_UNDERLINE

private data class CharFormat(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
)

private class FormatVisualTransformation(
    private val formats: List<CharFormat>,
) : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val styled = buildAnnotatedString {
            append(text)
            var i = 0
            while (i < text.length && i < formats.size) {
                val fmt = formats[i]
                var j = i + 1
                while (j < text.length && j < formats.size && formats[j] == fmt) j++
                if (fmt.bold || fmt.italic || fmt.underline) {
                    addStyle(
                        SpanStyle(
                            fontWeight = if (fmt.bold) FontWeight.Bold else null,
                            fontStyle = if (fmt.italic) FontStyle.Italic else null,
                            textDecoration = if (fmt.underline) TextDecoration.Underline else null,
                        ),
                        i, j,
                    )
                }
                i = j
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

private fun toIrc(text: String, formats: List<CharFormat>): String {
    if (text.isEmpty()) return text
    val sb = StringBuilder()
    var bold = false; var italic = false; var underline = false
    for (i in text.indices) {
        val fmt = if (i < formats.size) formats[i] else CharFormat()
        if (fmt.bold != bold) { sb.append(IRC_BOLD); bold = fmt.bold }
        if (fmt.italic != italic) { sb.append(IRC_ITALIC); italic = fmt.italic }
        if (fmt.underline != underline) { sb.append(IRC_UNDERLINE); underline = fmt.underline }
        sb.append(text[i])
    }
    if (bold) sb.append(IRC_BOLD)
    if (italic) sb.append(IRC_ITALIC)
    if (underline) sb.append(IRC_UNDERLINE)
    return sb.toString()
}

@Composable
fun MessageInput(
    onSend: (String) -> Unit,
) {
    var textField by remember { mutableStateOf(TextFieldValue()) }
    var charFormats by remember { mutableStateOf(listOf<CharFormat>()) }
    var activeFormat by remember { mutableStateOf(CharFormat()) }

    fun send() {
        val raw = textField.text
        if (raw.isBlank()) return
        val start = raw.indexOfFirst { !it.isWhitespace() }
        val end = raw.indexOfLast { !it.isWhitespace() } + 1
        onSend(toIrc(raw.substring(start, end), charFormats.subList(start, minOf(end, charFormats.size))))
        textField = TextFieldValue()
        charFormats = emptyList()
        activeFormat = CharFormat()
    }

    fun toggleFormat(
        getter: (CharFormat) -> Boolean,
        setter: (CharFormat, Boolean) -> CharFormat,
    ) {
        val sel = textField.selection
        if (sel.collapsed) {
            activeFormat = setter(activeFormat, !getter(activeFormat))
        } else {
            val allSet = (sel.min until sel.max).all { i ->
                charFormats.getOrNull(i)?.let(getter) == true
            }
            charFormats = charFormats.toMutableList().also { fmts ->
                for (i in sel.min until minOf(sel.max, fmts.size)) {
                    fmts[i] = setter(fmts[i], !allSet)
                }
            }
            activeFormat = setter(activeFormat, !allSet)
        }
    }

    fun handleValueChange(newValue: TextFieldValue) {
        val oldText = textField.text
        val newText = newValue.text

        if (newText != oldText) {
            val prefixLen = oldText.commonPrefixWith(newText).length
            val maxSuffix = minOf(oldText.length, newText.length) - prefixLen
            var suffixLen = 0
            while (suffixLen < maxSuffix &&
                oldText[oldText.length - 1 - suffixLen] == newText[newText.length - 1 - suffixLen]
            ) suffixLen++

            val newFormats = mutableListOf<CharFormat>()
            newFormats.addAll(charFormats.take(prefixLen))
            repeat(newText.length - prefixLen - suffixLen) { newFormats.add(activeFormat) }
            if (suffixLen > 0 && charFormats.size >= oldText.length) {
                newFormats.addAll(charFormats.subList(oldText.length - suffixLen, oldText.length))
            }
            while (newFormats.size < newText.length) newFormats.add(activeFormat)
            if (newFormats.size > newText.length) {
                newFormats.subList(newText.length, newFormats.size).clear()
            }
            charFormats = newFormats
        }

        textField = newValue

        if (newValue.selection.collapsed && charFormats.isNotEmpty()) {
            val pos = newValue.selection.start
            activeFormat = if (pos > 0) charFormats[pos - 1] else charFormats[0]
        }
    }

    Surface(tonalElevation = 3.dp) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FormatToggle("B", "Bold", activeFormat.bold, FontWeight.Bold) {
                    toggleFormat(CharFormat::bold) { f, v -> f.copy(bold = v) }
                }
                FormatToggle("I", "Italic", activeFormat.italic, fontStyle = FontStyle.Italic) {
                    toggleFormat(CharFormat::italic) { f, v -> f.copy(italic = v) }
                }
                FormatToggle("U", "Underline", activeFormat.underline,
                    textDecoration = TextDecoration.Underline) {
                    toggleFormat(CharFormat::underline) { f, v -> f.copy(underline = v) }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = textField,
                    onValueChange = ::handleValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    visualTransformation = FormatVisualTransformation(charFormats),
                )
                IconButton(onClick = { send() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatToggle(
    label: String,
    description: String,
    active: Boolean,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    textDecoration: TextDecoration? = null,
    onClick: () -> Unit,
) {
    val bg = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.textButtonColors(containerColor = bg),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                textDecoration = textDecoration,
            ),
        )
    }
}
