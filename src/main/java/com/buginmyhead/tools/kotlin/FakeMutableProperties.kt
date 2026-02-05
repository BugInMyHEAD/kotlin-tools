package com.buginmyhead.tools.kotlin

import kotlin.reflect.KMutableProperty0

fun <V> fakeMutableProperty0Of(fixedValue: V): KMutableProperty0<V> =
    object {
        var value: V
            get() = fixedValue
            set(_) = Unit
    }::value

fun <V> fakeMutableProperty0By(getter: () -> V): KMutableProperty0<V> =
    object {
        var value: V
            get() = getter()
            set(_) = Unit
    }::value