package io.legado.probe

import android.app.Activity
import android.os.Bundle
import android.util.Log

class WebViewProbeActivity : Activity() {

    private var server: ProbeHttpServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (server == null) {
            server = ProbeHttpServer(applicationContext, 18888).apply {
                start()
                Log.i("Probe", "Server started on port 18888")
            }
        }
    }

    override fun onDestroy() {
        server?.stop()
        server = null
        super.onDestroy()
    }
}
