package io.legado.probe

import android.content.Context
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking

class ProbeHttpServer(
    private val context: Context,
    port: Int = 18888
) : NanoHTTPD(port) {

    private val runner = WebViewRunner(context)
    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        return when {
            uri == "/render" && session.method == Method.POST -> handleRender(session)
            uri == "/ping" -> newFixedLengthResponse(Response.Status.OK, "text/plain", "pong")
            uri == "/info" -> handleInfo()
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                """{"error":"Not found"}""")
        }
    }

    private fun handleRender(session: IHTTPSession): Response {
        return try {
            val bodyMap = HashMap<String, String>()
            session.parseBody(bodyMap)
            val jsonBody = bodyMap["postData"] ?: return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, "application/json", """{"error":"Empty body"}""")

            val request = gson.fromJson(jsonBody, RenderRequest::class.java)
            val response = runBlocking { runner.render(request) }
            newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(response))
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                """{"ok":false,"error":"${e.message?.replace("\"", "\\\"")}"}""")
        }
    }

    private fun handleInfo(): Response {
        val info = mapOf(
            "name" to "legado-android-probe",
            "version" to "0.1.0",
            "api" to listOf("/render", "/ping", "/info")
        )
        return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(info))
    }
}
