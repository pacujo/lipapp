package com.lipapp.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lipapp.util.IRC_BOLD
import com.lipapp.util.IRC_ITALIC
import com.lipapp.util.IRC_RESET
import com.lipapp.util.IRC_UNDERLINE

@Composable
fun MessageInput(
    onSend: (String) -> Unit,
) {
    var textField by remember { mutableStateOf(TextFieldValue()) }

    fun send() {
        val text = textField.text.trim()
        if (text.isNotEmpty()) {
            onSend(text)
            textField = TextFieldValue()
        }
    }

    fun wrapSelection(code: Char) {
        val sel = textField.selection
        val t = textField.text
        if (sel.collapsed) {
            val new = t.substring(0, sel.start) + code + code + t.substring(sel.start)
            textField = TextFieldValue(new, TextRange(sel.start + 1))
        } else {
            val selected = t.substring(sel.min, sel.max)
            val new = t.substring(0, sel.min) + code + selected + code + t.substring(sel.max)
            textField = TextFieldValue(new, TextRange(sel.max + 2))
        }
    }

    Surface(tonalElevation = 3.dp) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FormattingButton("B", "Bold") { wrapSelection(IRC_BOLD) }
                FormattingButton("I", "Italic") { wrapSelection(IRC_ITALIC) }
                FormattingButton("U", "Underline") { wrapSelection(IRC_UNDERLINE) }
                FormattingButton("R", "Reset") {
                    val sel = textField.selection
                    val t = textField.text
                    val new = t.substring(0, sel.start) + IRC_RESET + t.substring(sel.start)
                    textField = TextFieldValue(new, TextRange(sel.start + 1))
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
                    onValueChange = { textField = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
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
private fun FormattingButton(label: String, description: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
