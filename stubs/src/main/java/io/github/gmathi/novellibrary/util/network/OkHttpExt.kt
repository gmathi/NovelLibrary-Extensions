package io.github.gmathi.novellibrary.util.network

import okhttp3.Call
import okhttp3.Response

fun Call.safeExecute(): Response {
    throw Exception("Stub!")
}