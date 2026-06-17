package io.legado.validator.web

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object CookieStore {
    private val cookies = ConcurrentHashMap<String, String>()
    private val gson = Gson()
    private val persistFile: File by lazy {
        val envPath = System.getenv("COOKIE_STORE")
        if (envPath != null) File(envPath)
        else File("validator-cookies.json")
    }

    init {
        loadFromDisk()
    }

    fun setCookie(domain: String, cookie: String) {
        cookies[domain.lowercase()] = cookie
        saveToDisk()
    }

    fun getCookie(domain: String): String? = cookies[domain.lowercase()]

    fun clearCookie(domain: String) {
        cookies.remove(domain.lowercase())
        saveToDisk()
    }

    fun clearAll() {
        cookies.clear()
        saveToDisk()
    }

    fun getAll(): Map<String, String> = cookies.toMap()

    fun hasCookies(): Boolean = cookies.isNotEmpty()

    private fun saveToDisk() {
        try {
            persistFile.writeText(gson.toJson(cookies.toMap()))
        } catch (_: Exception) {
            // 静默失败 — Cookie 持久化不是关键路径，文件写入失败不影响验证
        }
    }

    private fun loadFromDisk() {
        try {
            if (persistFile.exists()) {
                val json = persistFile.readText()
                val map: Map<String, String> = gson.fromJson(
                    json,
                    object : TypeToken<Map<String, String>>() {}.type
                )
                cookies.putAll(map)
            }
        } catch (_: Exception) {
            // 文件损坏或不可读时清空启动
            cookies.clear()
        }
    }
}
