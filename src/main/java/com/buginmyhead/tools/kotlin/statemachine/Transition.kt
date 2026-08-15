package com.buginmyhead.tools.kotlin.statemachine

data class Transition<S : TypeSafeBroker.Key<*>, F : Any>(
    val state: S,
    val globalEffect: F,
    val stateToEffect: TypeSafeBroker
)