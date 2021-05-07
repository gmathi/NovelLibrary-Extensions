package io.github.gmathi.novellibrary.util.system

fun String.encodeBase64ToString(): String = String(this.toByteArray().encodeBase64())
fun String.encodeBase64ToByteArray(): ByteArray = this.toByteArray().encodeBase64()
fun ByteArray.encodeBase64ToString(): String = String(this.encodeBase64())

fun String.decodeBase64(): String = String(this.toByteArray().decodeBase64())
fun String.decodeBase64ToByteArray(): ByteArray = this.toByteArray().decodeBase64()
fun ByteArray.decodeBase64ToString(): String = String(this.decodeBase64())

fun ByteArray.encodeBase64(): ByteArray {
    throw Exception("Stub!")
}

fun ByteArray.decodeBase64(): ByteArray {
    throw Exception("Stub!")
}