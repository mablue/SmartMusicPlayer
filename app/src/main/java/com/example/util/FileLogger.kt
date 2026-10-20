package com.example.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persistent file-based logger.
 *
 * - Writes to: /sdcard/Android/data/<pkg>/files/smartmusic_logs/errors.log
 * - Appends (does NOT truncate) — previous session logs are preserved
 * - Auto-creates the directory/file if missing
 * - Rotates if file exceeds 2MB (keeps last ~1MB)
 * - Installs a global UncaughtExceptionHandler so crashes are logged
 */
object FileLogger {
    private const val TAG = "FileLogger"
    private const val LOG_DIR = "smartmusic_logs"
    private const val LOG_FILE = "errors.log"
    private const val MAX_BYTES = 2 * 1024 * 1024L

    @Volatile private var file: File? = null
    private val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile var initialized = false
        private set

    fun init(context: Context) {
        if (initialized) return
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, LOG_DIR)
        if (!dir.exists()) dir.mkdirs()
        val f = File(dir, LOG_FILE)
        file = f

        // Rotate if too large
        if (f.exists() && f.length() > MAX_BYTES) {
            try {
                val tail = f.readText().takeLast((MAX_BYTES / 2).toInt())
                f.writeText("[ROTATED at ${now()}]\n$tail")
            } catch (_: Exception) {}
        }

        // Install crash handler
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            error("CRASH", "Uncaught on thread=${thread.name}", throwable)
            prev?.uncaughtException(thread, throwable)
        }

        // Session header
        appendLine("")
        appendLine("=========== SESSION ${now()} ===========")
        appendLine("Device  : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        appendLine("Android : ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        appendLine("Package : ${context.packageName}")
        initialized = true
        Log.d(TAG, "FileLogger initialized: ${f.absolutePath}")
    }

    fun info(tag: String, msg: String) = appendLine("[$tag] ${now()} $msg")

    fun warn(tag: String, msg: String, e: Throwable? = null) {
        val stack = e?.let { "\n" + toStack(it) } ?: ""
        appendLine("[$tag][WARN] ${now()} $msg$stack")
    }

    fun error(tag: String, msg: String, e: Throwable? = null) {
        val stack = e?.let { "\n" + toStack(it) } ?: ""
        appendLine("[$tag][ERROR] ${now()} $msg$stack")
    }

    fun getLogFile(): File? = file
    fun getLogPath(): String? = file?.absolutePath
    fun readAll(): String = try { file?.readText() ?: "(no log file)" } catch (e: Exception) { "(read error: ${e.message})" }
    fun clear() { try { file?.writeText("[CLEARED ${now()}]\n") } catch (_: Exception) {} }

    private fun appendLine(line: String) {
        val f = file ?: return
        try { f.appendText(line + "\n") } catch (e: Exception) { Log.e(TAG, "write failed: ${e.message}") }
    }

    private fun now() = df.format(Date())
    private fun toStack(e: Throwable) = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString()
}
