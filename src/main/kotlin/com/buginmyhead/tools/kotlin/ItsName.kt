package com.buginmyhead.tools.kotlin

import kotlin.reflect.KProperty

object ItsName {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = property.name

}