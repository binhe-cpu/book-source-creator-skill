package io.legado.probe

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

class WebViewRunner(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun render(request: RenderRequest): RenderResponse = withContext(Dispatchers.Main) {
        val startTime = System.currentTimeMillis()
        try {
            withTimeout(request.timeout) {
                suspendCancellableCoroutine { cont ->
                    val webView = WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            blockNetworkImage = true
                            request.headers["User-Agent"]?.let { userAgentString = it }
                        }
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            val cookies = CookieManager.getInstance().getCookie(url)
                            handler.postDelayed({
                                executeJsAndFinish(
                                    view, url, cookies, request, cont,
                                    System.currentTimeMillis() - startTime
                                )
                            }, request.jsDelay)
                        }

                        override fun onReceivedSslError(
                            view: WebView, sslHandler: SslErrorHandler, error: SslError
                        ) {
                            sslHandler.proceed()
                        }

                        override fun onReceivedError(
                            view: WebView, req: WebResourceRequest, error: WebResourceError
                        ) {
                            if (req.isForMainFrame && !cont.isCompleted) {
                                cont.resume(
                                    RenderResponse(
                                        ok = false,
                                        error = "Page load error: ${error.description}",
                                        loadTimeMs = System.currentTimeMillis() - startTime
                                    )
                                )
                            }
                        }
                    }

                    cont.invokeOnCancellation { webView.destroy() }
                    webView.loadUrl(request.url, request.headers)
                }
            }
        } catch (e: TimeoutCancellationException) {
            RenderResponse(ok = false, error = "Timeout after ${request.timeout}ms", loadTimeMs = request.timeout)
        } catch (e: Exception) {
            RenderResponse(ok = false, error = "${e::class.simpleName}: ${e.message}")
        }
    }

    private fun executeJsAndFinish(
        webView: WebView, url: String?, cookies: String?,
        request: RenderRequest, cont: CancellableCoroutine<RenderResponse>,
        elapsed: Long
    ) {
        val js = request.javaScript ?: "document.documentElement.outerHTML"
        var retryCount = 0

        fun tryExecute() {
            webView.evaluateJavascript(js) { result ->
                val html = result?.removeSurrounding("\"")?.unescapeJson()
                if (!html.isNullOrEmpty() && html != "null") {
                    val screenshot = if (request.screenshot) captureScreenshot(webView) else null
                    if (!cont.isCompleted) {
                        cont.resume(
                            RenderResponse(
                                ok = true,
                                html = html,
                                finalUrl = webView.url,
                                title = webView.title,
                                cookies = cookies,
                                screenshotBase64 = screenshot,
                                loadTimeMs = elapsed
                            )
                        )
                    }
                    webView.destroy()
                } else if (retryCount < request.jsRetries) {
                    retryCount++
                    handler.postDelayed({ tryExecute() }, request.jsDelay)
                } else {
                    if (!cont.isCompleted) {
                        cont.resume(
                            RenderResponse(
                                ok = false,
                                error = "JS execution timeout after ${request.jsRetries} retries",
                                html = html,
                                finalUrl = webView.url,
                                cookies = cookies,
                                loadTimeMs = elapsed
                            )
                        )
                    }
                    webView.destroy()
                }
            }
        }
        tryExecute()
    }

    private fun captureScreenshot(webView: WebView): String? {
        return try {
            val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            webView.draw(canvas)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) { null }
    }

    private fun String.unescapeJson(): String {
        return replace("\\n", "\n").replace("\\t", "\t")
            .replace("\\\"", "\"").replace("\\\\", "\\")
            .replace("\\u003C", "<").replace("\\u003E", ">")
            .replace("\\u0026", "&")
    }
}
