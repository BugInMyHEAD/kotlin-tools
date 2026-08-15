package com.buginmyhead.tools.kotlin.statemachine

data class Transition<R : TypeSafeBroker.Key<*>, F : Any>(
    val state: R,
    val globalEffect: F,
    val stateToEffect: TypeSafeBroker
)