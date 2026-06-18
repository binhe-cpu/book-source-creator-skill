package io.legado.probe

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView

class WebViewProbeActivity : Activity() {

    private var server: ProbeHttpServer? = null
    private lateinit var statusText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var requestCount = 0

    companion object {
        var instance: WebViewProbeActivity? = null
            private set
    }

    fun updateStatus(message: String) {
        handler.post {
            requestCount++
            val display = buildString {
                append("Legado Android Probe\n")
                append("Port: 18888 | Requests: $requestCount\n")
                append("──────────────────────\n")
                append(message)
            }
            statusText.text = display
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        statusText = TextView(this).apply {
            text = "Legado Android Probe\nPort: 18888\n──────────────────────\n等待请求..."
            textSize = 14f
            setPadding(32, 48, 32, 32)
        }
        setContentView(statusText)
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
