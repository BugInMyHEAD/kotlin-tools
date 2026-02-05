package com.buginmyhead.tools.kotlin

suspend inline fun <T> Iterable<T>.forEachCo(block: suspend (T) -> Unit): Unit =
    forEach { block(it) }