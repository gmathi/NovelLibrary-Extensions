package io.github.gmathi.novellibrary.network

import android.content.Context
import android.net.ConnectivityManager
import okhttp3.OkHttpClient

class NetworkHelper(private val context: Context) {

    val client: OkHttpClient = throw Exception("Stub!")

    @Suppress("ThrowableNotThrown")
    val cloudflareClient: OkHttpClient = throw Exception("Stub!")

    /**
     * returns - True - if there is connection to the internet
     */
    fun isConnectedToNetwork(): Boolean = throw Exception("Stub!")
}
