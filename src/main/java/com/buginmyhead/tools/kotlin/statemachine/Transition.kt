package com.buginmyhead.tools.kotlin.statemachine

data class Transition<S : TypeSafeBroker.Key<*>>(
    val state: S,
    val stateToEffect: TypeSafeBroker = TypeSafeBroker()
)