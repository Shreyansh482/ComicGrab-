package com.example.data.repository

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val isError: Boolean = false,
    val details: String? = null
)

object DownloadDiagnostics {
    private val _logs = MutableStateFlow<List<DiagnosticEntry>>(emptyList())
    val logs: StateFlow<List<DiagnosticEntry>> = _logs.asStateFlow()

    private const val MAX_LOGS = 60

    fun log(tag: String, message: String, isError: Boolean = false, details: String? = null) {
        val entry = DiagnosticEntry(
            tag = tag,
            message = message,
            isError = isError,
            details = details
        )
        val current = _logs.value.toMutableList()
        current.add(0, entry) // Latest first
        if (current.size > MAX_LOGS) {
            _logs.value = current.take(MAX_LOGS)
        } else {
            _logs.value = current
        }
    }

    fun getFormattedSummary(): String {
        val sb = StringBuilder()
        sb.append("=== ComicGrab Diagnostic Report ===\n")
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        for (log in _logs.value.reversed()) {
            val time = sdf.format(Date(log.timestamp))
            val prefix = if (log.isError) "[ERROR]" else "[INFO]"
            sb.append("$time $prefix [${log.tag}] ${log.message}\n")
            if (!log.details.isNullOrBlank()) {
                sb.append("   Details: ${log.details}\n")
            }
        }
        sb.append("===================================")
        return sb.toString()
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
