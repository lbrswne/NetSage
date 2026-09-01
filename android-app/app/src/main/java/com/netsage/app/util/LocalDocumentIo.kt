package com.netsage.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object LocalDocumentIo {
    /** Shared limit for imported, shared, manually entered, and persisted raw logs. */
    const val MAX_LOG_CHARS = 100_000

    data class ImportedText(
        val text: String,
        val truncated: Boolean,
    )

    fun limitLogText(text: String): ImportedText = ImportedText(
        text = text.take(MAX_LOG_CHARS),
        truncated = text.length > MAX_LOG_CHARS,
    )

    fun readText(context: Context, uri: Uri): Result<ImportedText> = runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            val buffer = CharArray(8_192)
            val result = StringBuilder()
            while (result.length < MAX_LOG_CHARS) {
                val read = reader.read(buffer, 0, minOf(buffer.size, MAX_LOG_CHARS - result.length))
                if (read <= 0) break
                result.append(buffer, 0, read)
            }
            val truncated = result.length >= MAX_LOG_CHARS && reader.read() != -1
            ImportedText(result.toString(), truncated)
        } ?: error("无法读取所选文件")
    }

    fun writeText(context: Context, uri: Uri, content: String): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.write(content)
        } ?: error("无法写入所选文件")
    }

    fun shareText(context: Context, subject: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, "分享 NetSage 诊断会话"))
    }
}
