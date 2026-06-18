package io.legado.probe

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.ScrollView
import android.widget.TextView

class WebViewProbeActivity : Activity() {

    private var server: ProbeHttpServer? = null
    private lateinit var logText: TextView
    private lateinit var scrollView: ScrollView
    private val handler = Handler(Looper.getMainLooper())
    private var requestCount = 0
    private val logLines = mutableListOf<String>()

    companion object {
        var instance: WebViewProbeActivity? = null
            private set
    }

    fun updateStatus(message: String) {
        handler.post {
            requestCount++
            val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            val line = "$ts | #$requestCount | $message"
            logLines.add(line)
            // Keep last 50 entries
            while (logLines.size > 50) logLines.removeAt(0)
            refreshDisplay()
        }
    }

    private fun refreshDisplay() {
        val sb = StringBuilder()
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("  Legado Android Probe\n")
        sb.append("  Port: 18888 | Total: $requestCount\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n")
        logLines.forEach { sb.append(it).append("\n") }
        logText.text = sb.toString()
        // Auto-scroll to bottom
        scrollView.post { scrollView.fullScroll(android.view.View.FOCUS_DOWN) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        logText = TextView(this).apply {
            text = "━━━━━━━━━━━━━━━━━━━━━━\n  Legado Android Probe\n  Port: 18888 | Total: 0\n━━━━━━━━━━━━━━━━━━━━━━\n等待请求..."
            textSize = 13f
            setPadding(24, 32, 24, 32)
            setTextColor(Color.parseColor("#E0E0E0"))
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            gravity = Gravity.START
        }

        scrollView = ScrollView(this).apply {
            addView(logText)
            setBackgroundColor(Color.parseColor("#1A1A2E"))
        }
        setContentView(scrollView)

        if (server == null) {
            server = ProbeHttpServer(applicationContext, 18888).apply {
                start()
                Log.i("Probe", "Server started on port 18888")
            }
        }
    }

    override fun onDestroy() {
        instance = null
        server?.stop()
        server = null
        super.onDestroy()
    }
}
