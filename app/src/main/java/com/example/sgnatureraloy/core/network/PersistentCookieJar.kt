package com.example.sgnatureraloy.core.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class PersistentCookieJar(context: Context) : CookieJar {
    private val sharedPreferences = context.getSharedPreferences("cookie_prefs", Context.MODE_PRIVATE)
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    init {
        // Load existing cookies from SharedPreferences
        sharedPreferences.all.forEach { (host, value) ->
            if (value is Set<*>) {
                val cookieList = value.mapNotNull { 
                    val cookieString = it as? String ?: return@mapNotNull null
                    // Note: Cookie.parse requires the URL. This is a simplified version.
                    // In a real production app, we would store parts of the cookie separately.
                    // For this task, we assume the host is enough to reconstruct.
                    Cookie.parse(HttpUrl.Builder().scheme("https").host(host).build(), cookieString)
                }.toMutableList()
                cookieStore[host] = cookieList
            }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        cookieStore[host] = cookies.toMutableList()
        
        val cookieStrings = cookies.map { it.toString() }.toSet()
        sharedPreferences.edit().putStringSet(host, cookieStrings).apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }
}
