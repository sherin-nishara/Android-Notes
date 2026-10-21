package com.example.floatingnote

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.*

object MarkdownRenderer {
    fun render(context: Context, markdown: String): CharSequence {
        val builder = SpannableStringBuilder()
        val lines = markdown.split("\n")
        var inCodeBlock = false
        val codeBlock = StringBuilder()

        for (line in lines) {
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    // End code block
                    val code = codeBlock.toString().trimEnd('\n')
                    val start = builder.length
                    builder.append(code)
                    builder.setSpan(TypefaceSpan("monospace"), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(BackgroundColorSpan(0x20808080), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    codeBlock.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlock.append(line).append("\n")
                continue
            }

            // Headers
            val headerMatch = Regex("^(#{1,3})\\s+(.+)$").find(line.trim())
            if (headerMatch != null) {
                val level = headerMatch.groupValues[1].length
                val text = headerMatch.groupValues[2]
                val start = builder.length
                builder.append(text).append("\n")
                val size = when (level) {
                    1 -> 1.5f
                    2 -> 1.3f
                    else -> 1.15f
                }
                builder.setSpan(RelativeSizeSpan(size), start, builder.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                continue
            }

            // Blockquote
            if (line.trim().startsWith("> ")) {
                val text = line.trim().removePrefix("> ")
                val start = builder.length
                builder.append("  ").append(text).append("\n")
                builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(ForegroundColorSpan(0x808080), start, builder.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                continue
            }

            // Bullet list
            val bulletMatch = Regex("^[-*+]\\s+(.+)$").find(line.trim())
            if (bulletMatch != null) {
                val text = bulletMatch.groupValues[1]
                val start = builder.length
                builder.append("• ").append(renderInline(text)).append("\n")
                continue
            }

            // Numbered list
            val numberedMatch = Regex("^\\d+\\.\\s+(.+)$").find(line.trim())
            if (numberedMatch != null) {
                val text = numberedMatch.groupValues[1]
                val start = builder.length
                builder.append("  ").append(renderInline(text)).append("\n")
                continue
            }

            // Horizontal rule
            if (line.trim() == "---" || line.trim() == "***" || line.trim() == "___") {
                builder.append("───────────────\n")
                continue
            }

            // Empty line
            if (line.trim().isEmpty()) {
                builder.append("\n")
                continue
            }

            // Regular paragraph
            builder.append(renderInline(line)).append("\n")
        }

        return builder
    }

    private fun renderInline(text: String): CharSequence {
        val result = SpannableStringBuilder()
        var i = 0
        val len = text.length

        while (i < len) {
            // Bold **text** or __text__
            if (i + 2 <= len && (text.startsWith("**", i) || text.startsWith("__", i))) {
                val marker = text.substring(i, i + 2)
                val end = text.indexOf(marker, i + 2)
                if (end != -1) {
                    val boldText = text.substring(i + 2, end)
                    val start = result.length
                    result.append(boldText)
                    result.setSpan(StyleSpan(Typeface.BOLD), start, result.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = end + 2
                    continue
                }
            }

            // Italic *text* or _text_
            if (i < len && (text[i] == '*' || text[i] == '_') && (i == 0 || text[i-1] != text[i])) {
                val marker = text[i].toString()
                val end = text.indexOf(marker, i + 1)
                if (end != -1 && end > i + 1) {
                    val italicText = text.substring(i + 1, end)
                    val start = result.length
                    result.append(italicText)
                    result.setSpan(StyleSpan(Typeface.ITALIC), start, result.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = end + 1
                    continue
                }
            }

            // Inline code `text`
            if (i < len && text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    val codeText = text.substring(i + 1, end)
                    val start = result.length
                    result.append(codeText)
                    result.setSpan(TypefaceSpan("monospace"), start, result.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    result.setSpan(BackgroundColorSpan(0x20808080), start, result.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = end + 1
                    continue
                }
            }

            // Link [text](url)
            if (i < len && text[i] == '[') {
                val closeBracket = text.indexOf(']', i)
                if (closeBracket != -1 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        val start = result.length
                        result.append(linkText)
                        result.setSpan(URLSpan(url), start, result.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        result.setSpan(ForegroundColorSpan(0x26A269), start, result.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        result.setSpan(UnderlineSpan(), start, result.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = closeParen + 1
                        continue
                    }
                }
            }

            // Strikethrough ~~text~~
            if (i + 2 <= len && text.startsWith("~~", i)) {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    val strikeText = text.substring(i + 2, end)
                    val start = result.length
                    result.append(strikeText)
                    result.setSpan(StrikethroughSpan(), start, result.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = end + 2
                    continue
                }
            }

            result.append(text[i])
            i++
        }

        return result
    }
}
