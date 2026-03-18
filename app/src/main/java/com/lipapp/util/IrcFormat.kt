package com.lipapp.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

private val IRC_COLORS = arrayOf(
    Color.White,            // 0
    Color.Black,            // 1
    Color(0xFF00007F),      // 2  navy
    Color(0xFF009300),      // 3  green
    Color(0xFFFF0000),      // 4  red
    Color(0xFF7F0000),      // 5  brown
    Color(0xFF9C009C),      // 6  purple
    Color(0xFFFC7F00),      // 7  orange
    Color(0xFFFFFF00),      // 8  yellow
    Color(0xFF00FC00),      // 9  lime
    Color(0xFF009393),      // 10 teal
    Color(0xFF00FFFF),      // 11 cyan
    Color(0xFF0000FC),      // 12 blue
    Color(0xFFFF00FF),      // 13 pink
    Color(0xFF7F7F7F),      // 14 grey
    Color(0xFFD2D2D2),      // 15 silver
    Color(0xFF470000), Color(0xFF472100), Color(0xFF474700), Color(0xFF324700), // 16-19
    Color(0xFF004700), Color(0xFF00472C), Color(0xFF004747), Color(0xFF002747), // 20-23
    Color(0xFF000047), Color(0xFF2E0047), Color(0xFF470047), Color(0xFF47002A), // 24-27
    Color(0xFF740000), Color(0xFF743A00), Color(0xFF747400), Color(0xFF517400), // 28-31
    Color(0xFF007400), Color(0xFF007449), Color(0xFF007474), Color(0xFF004074), // 32-35
    Color(0xFF000074), Color(0xFF4B0074), Color(0xFF740074), Color(0xFF740045), // 36-39
    Color(0xFFB50000), Color(0xFFB56300), Color(0xFFB5B500), Color(0xFF7DB500), // 40-43
    Color(0xFF00B500), Color(0xFF00B571), Color(0xFF00B5B5), Color(0xFF0063B5), // 44-47
    Color(0xFF0000B5), Color(0xFF7500B5), Color(0xFFB500B5), Color(0xFFB5006B), // 48-51
    Color(0xFFFF0000), Color(0xFFFF8C00), Color(0xFFFFFF00), Color(0xFFB2FF00), // 52-55
    Color(0xFF00FF00), Color(0xFF00FFA0), Color(0xFF00FFFF), Color(0xFF008CFF), // 56-59
    Color(0xFF0000FF), Color(0xFFA500FF), Color(0xFFFF00FF), Color(0xFFFF0098), // 60-63
    Color(0xFFFF5959), Color(0xFFFFB459), Color(0xFFFFFF71), Color(0xFFCFFF60), // 64-67
    Color(0xFF6FFF6F), Color(0xFF65FFC9), Color(0xFF6DFFFF), Color(0xFF59B4FF), // 68-71
    Color(0xFF5959FF), Color(0xFFC459FF), Color(0xFFFF66FF), Color(0xFFFF59BC), // 72-75
    Color(0xFFFF9C9C), Color(0xFFFFD39C), Color(0xFFFFFF9C), Color(0xFFE2FF9C), // 76-79
    Color(0xFF9CFF9C), Color(0xFF9CFFDB), Color(0xFF9CFFFF), Color(0xFF9CD3FF), // 80-83
    Color(0xFF9C9CFF), Color(0xFFDC9CFF), Color(0xFFFF9CFF), Color(0xFFFF94D3), // 84-87
    Color(0xFF000000), Color(0xFF131313), Color(0xFF282828), Color(0xFF363636), // 88-91
    Color(0xFF4D4D4D), Color(0xFF656565), Color(0xFF818181), Color(0xFF9F9F9F), // 92-95
    Color(0xFFBCBCBC), Color(0xFFE2E2E2), Color(0xFFFFFFFF),                   // 96-98
)

private data class FormatState(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val monospace: Boolean = false,
    val fg: Color? = null,
    val bg: Color? = null,
    val reverse: Boolean = false,
)

private fun FormatState.toSpanStyle(): SpanStyle {
    val effectiveFg = if (reverse) bg else fg
    val effectiveBg = if (reverse) fg else bg
    val decorations = mutableListOf<TextDecoration>()
    if (underline) decorations += TextDecoration.Underline
    if (strikethrough) decorations += TextDecoration.LineThrough

    return SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        fontFamily = if (monospace) FontFamily.Monospace else null,
        color = effectiveFg ?: Color.Unspecified,
        background = effectiveBg ?: Color.Unspecified,
        textDecoration = if (decorations.isNotEmpty())
            decorations.reduce { a, b -> a + b } else null,
    )
}

private fun getIrcColor(index: Int): Color? =
    IRC_COLORS.getOrNull(index)

private fun parseColorNumber(text: String, pos: Int): Pair<Int?, Int> {
    if (pos >= text.length || !text[pos].isDigit()) return null to 0
    val end = (pos + 2).coerceAtMost(text.length)
    val numStr = text.substring(pos, end).takeWhile { it.isDigit() }
    return if (numStr.isNotEmpty()) numStr.toInt() to numStr.length else null to 0
}

private fun parseHexColor(text: String, pos: Int): Color? {
    if (pos + 6 > text.length) return null
    val hex = text.substring(pos, pos + 6)
    return try {
        Color(("FF$hex").toLong(16))
    } catch (_: NumberFormatException) {
        null
    }
}

fun parseIrcFormat(text: String): AnnotatedString = buildAnnotatedString {
    var state = FormatState()
    val buf = StringBuilder()

    fun flush() {
        if (buf.isNotEmpty()) {
            val style = state.toSpanStyle()
            if (style == SpanStyle()) {
                append(buf)
            } else {
                withStyle(style) { append(buf) }
            }
            buf.clear()
        }
    }

    var i = 0
    while (i < text.length) {
        when (text[i].code) {
            0x02 -> { flush(); state = state.copy(bold = !state.bold); i++ }
            0x1D -> { flush(); state = state.copy(italic = !state.italic); i++ }
            0x1F -> { flush(); state = state.copy(underline = !state.underline); i++ }
            0x1E -> { flush(); state = state.copy(strikethrough = !state.strikethrough); i++ }
            0x11 -> { flush(); state = state.copy(monospace = !state.monospace); i++ }
            0x16 -> { flush(); state = state.copy(reverse = !state.reverse); i++ }
            0x0F -> { flush(); state = FormatState(); i++ }
            0x03 -> {
                flush(); i++
                val (fg, c1) = parseColorNumber(text, i)
                i += c1
                if (i < text.length && text[i] == ',') {
                    i++
                    val (bg, c2) = parseColorNumber(text, i)
                    i += c2
                    state = state.copy(
                        fg = fg?.let { getIrcColor(it) } ?: state.fg,
                        bg = bg?.let { getIrcColor(it) } ?: state.bg,
                    )
                } else {
                    if (fg != null) {
                        state = state.copy(fg = getIrcColor(fg))
                    } else {
                        state = state.copy(fg = null, bg = null)
                    }
                }
            }
            0x04 -> {
                flush(); i++
                val hexFg = parseHexColor(text, i)
                if (hexFg != null) {
                    i += 6
                    state = state.copy(fg = hexFg)
                    if (i < text.length && text[i] == ',') {
                        i++
                        val hexBg = parseHexColor(text, i)
                        if (hexBg != null) {
                            i += 6
                            state = state.copy(bg = hexBg)
                        }
                    }
                } else {
                    state = state.copy(fg = null, bg = null)
                }
            }
            else -> { buf.append(text[i]); i++ }
        }
    }
    flush()
}

const val IRC_BOLD = '\u0002'
const val IRC_ITALIC = '\u001D'
const val IRC_UNDERLINE = '\u001F'
const val IRC_RESET = '\u000F'
